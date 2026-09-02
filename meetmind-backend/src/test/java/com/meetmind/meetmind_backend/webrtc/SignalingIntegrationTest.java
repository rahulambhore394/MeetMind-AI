package com.meetmind.meetmind_backend.webrtc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import com.meetmind.meetmind_backend.webrtc.dto.SignalingMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
public class SignalingIntegrationTest {

    @LocalServerPort
    private int port;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private User hostUser;
    private User participantUser;
    private User nonParticipantUser;
    private Meeting meeting;
    private String hostToken;
    private String participantToken;
    private String nonParticipantToken;

    private WebSocketStompClient stompClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        // Create Users
        hostUser = new User();
        hostUser.setName("Host User");
        hostUser.setEmail("host@webrtc.com");
        hostUser.setPassword("password");
        hostUser = userRepository.save(hostUser);

        participantUser = new User();
        participantUser.setName("Participant User");
        participantUser.setEmail("participant@webrtc.com");
        participantUser.setPassword("password");
        participantUser = userRepository.save(participantUser);

        nonParticipantUser = new User();
        nonParticipantUser.setName("Non-Participant User");
        nonParticipantUser.setEmail("stranger@webrtc.com");
        nonParticipantUser.setPassword("password");
        nonParticipantUser = userRepository.save(nonParticipantUser);

        // Generate tokens
        hostToken = jwtService.generateToken(hostUser.getId(), hostUser.getEmail());
        participantToken = jwtService.generateToken(participantUser.getId(), participantUser.getEmail());
        nonParticipantToken = jwtService.generateToken(nonParticipantUser.getId(), nonParticipantUser.getEmail());

        // Create Meeting and start it LIVE
        meeting = new Meeting();
        meeting.setTitle("WebRTC Live Meeting");
        meeting.setDescription("Live A/V Streaming Test");
        meeting.setHost(hostUser);
        meeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        meeting.setStatus(MeetingStatus.LIVE);
        meeting = meetingRepository.save(meeting);

        // Save host participant
        MeetingParticipant hostPart = new MeetingParticipant();
        hostPart.setMeeting(meeting);
        hostPart.setUser(hostUser);
        hostPart.setRole(ParticipantRole.HOST);
        hostPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(hostPart);

        // Save attendee participant
        MeetingParticipant partPart = new MeetingParticipant();
        partPart.setMeeting(meeting);
        partPart.setUser(participantUser);
        partPart.setRole(ParticipantRole.PARTICIPANT);
        partPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(partPart);

        // Configure Stomp Client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testWebRtcIceServers_Unauthenticated_Returns403() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
                "/api/webrtc/ice-servers",
                Object.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testWebRtcIceServers_Authenticated_ReturnsIceServers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object[]> response = restTemplate.exchange(
                "/api/webrtc/ice-servers",
                HttpMethod.GET,
                entity,
                Object[].class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void testSignaling_Success_Flow() throws Exception {
        // Connect Peer A (Host)
        StompHeaders headersA = new StompHeaders();
        headersA.add("Authorization", "Bearer " + hostToken);
        CompletableFuture<StompSession> sessionFutureA = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                headersA,
                new StompSessionHandlerAdapter() {}
        );
        StompSession sessionA = sessionFutureA.get(5, TimeUnit.SECONDS);
        assertTrue(sessionA.isConnected());

        // Connect Peer B (Participant)
        StompHeaders headersB = new StompHeaders();
        headersB.add("Authorization", "Bearer " + participantToken);
        CompletableFuture<StompSession> sessionFutureB = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                headersB,
                new StompSessionHandlerAdapter() {}
        );
        StompSession sessionB = sessionFutureB.get(5, TimeUnit.SECONDS);
        assertTrue(sessionB.isConnected());

        // Queue to collect messages for A and B
        LinkedBlockingQueue<SignalingMessage> queueA_Broadcast = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<SignalingMessage> queueA_Private = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<SignalingMessage> queueB_Broadcast = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<SignalingMessage> queueB_Private = new LinkedBlockingQueue<>();

        // A Subscriptions
        sessionA.subscribe("/topic/meetings/" + meeting.getId() + "/signaling", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return SignalingMessage.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queueA_Broadcast.offer((SignalingMessage) payload);
            }
        });
        sessionA.subscribe("/user/queue/meetings/" + meeting.getId() + "/signaling", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return SignalingMessage.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queueA_Private.offer((SignalingMessage) payload);
            }
        });

        // B Subscriptions
        sessionB.subscribe("/topic/meetings/" + meeting.getId() + "/signaling", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return SignalingMessage.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queueB_Broadcast.offer((SignalingMessage) payload);
            }
        });
        sessionB.subscribe("/user/queue/meetings/" + meeting.getId() + "/signaling", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return SignalingMessage.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queueB_Private.offer((SignalingMessage) payload);
            }
        });

        // Small delay for subscriptions registration on broker
        Thread.sleep(1000);

        // A sends JOIN
        SignalingMessage joinMsgA = new SignalingMessage(meeting.getId(), null, null, null, "JOIN", null);
        sessionA.send("/app/meetings/" + meeting.getId() + "/signaling", joinMsgA);

        // A gets PEER_LIST containing empty peers (since B hasn't joined yet)
        SignalingMessage peerListA = queueA_Private.poll(5, TimeUnit.SECONDS);
        assertNotNull(peerListA);
        assertEquals("PEER_LIST", peerListA.getType());
        assertEquals("", peerListA.getPayload());

        // B sends JOIN
        SignalingMessage joinMsgB = new SignalingMessage(meeting.getId(), null, null, null, "JOIN", null);
        sessionB.send("/app/meetings/" + meeting.getId() + "/signaling", joinMsgB);

        // B gets PEER_LIST containing Host User (A's ID)
        SignalingMessage peerListB = queueB_Private.poll(5, TimeUnit.SECONDS);
        assertNotNull(peerListB);
        assertEquals("PEER_LIST", peerListB.getType());
        assertTrue(peerListB.getPayload().contains(hostUser.getId().toString()));

        // B receives A's broadcast JOIN message
        SignalingMessage joinBroadcastB1 = queueB_Broadcast.poll(5, TimeUnit.SECONDS);
        assertNotNull(joinBroadcastB1);
        assertEquals("JOIN", joinBroadcastB1.getType());
        assertEquals(hostUser.getId(), joinBroadcastB1.getSenderId());

        // B receives B's own broadcast JOIN message
        SignalingMessage joinBroadcastB2 = queueB_Broadcast.poll(5, TimeUnit.SECONDS);
        assertNotNull(joinBroadcastB2);
        assertEquals("JOIN", joinBroadcastB2.getType());
        assertEquals(participantUser.getId(), joinBroadcastB2.getSenderId());

        // A receives A's own broadcast JOIN message
        SignalingMessage joinBroadcastA1 = queueA_Broadcast.poll(5, TimeUnit.SECONDS);
        assertNotNull(joinBroadcastA1);
        assertEquals("JOIN", joinBroadcastA1.getType());
        assertEquals(hostUser.getId(), joinBroadcastA1.getSenderId());

        // A receives B's broadcast JOIN message
        SignalingMessage joinBroadcastA2 = queueA_Broadcast.poll(5, TimeUnit.SECONDS);
        assertNotNull(joinBroadcastA2);
        assertEquals("JOIN", joinBroadcastA2.getType());
        assertEquals(participantUser.getId(), joinBroadcastA2.getSenderId());

        // A sends OFFER to B
        SignalingMessage offer = new SignalingMessage(meeting.getId(), null, null, participantUser.getId(), "OFFER", "SDP-Offer-Details");
        sessionA.send("/app/meetings/" + meeting.getId() + "/signaling", offer);

        // B receives the OFFER on private queue
        SignalingMessage recOffer = queueB_Private.poll(5, TimeUnit.SECONDS);
        assertNotNull(recOffer);
        assertEquals("OFFER", recOffer.getType());
        assertEquals(hostUser.getId(), recOffer.getSenderId());
        assertEquals("SDP-Offer-Details", recOffer.getPayload());

        // B sends ANSWER to A
        SignalingMessage answer = new SignalingMessage(meeting.getId(), null, null, hostUser.getId(), "ANSWER", "SDP-Answer-Details");
        sessionB.send("/app/meetings/" + meeting.getId() + "/signaling", answer);

        // A receives the ANSWER on private queue
        SignalingMessage recAnswer = queueA_Private.poll(5, TimeUnit.SECONDS);
        assertNotNull(recAnswer);
        assertEquals("ANSWER", recAnswer.getType());
        assertEquals(participantUser.getId(), recAnswer.getSenderId());
        assertEquals("SDP-Answer-Details", recAnswer.getPayload());

        // A sends ICE_CANDIDATE to B
        SignalingMessage candidate = new SignalingMessage(meeting.getId(), null, null, participantUser.getId(), "ICE_CANDIDATE", "Candidate-Details");
        sessionA.send("/app/meetings/" + meeting.getId() + "/signaling", candidate);

        // B receives the ICE candidate
        SignalingMessage recCandidate = queueB_Private.poll(5, TimeUnit.SECONDS);
        assertNotNull(recCandidate);
        assertEquals("ICE_CANDIDATE", recCandidate.getType());
        assertEquals(hostUser.getId(), recCandidate.getSenderId());
        assertEquals("Candidate-Details", recCandidate.getPayload());

        // A disconnects (abrupt leave)
        sessionA.disconnect();

        // B receives the LEAVE broadcast for A
        SignalingMessage leaveBroadcast = queueB_Broadcast.poll(5, TimeUnit.SECONDS);
        assertNotNull(leaveBroadcast);
        assertEquals("LEAVE", leaveBroadcast.getType());
        assertEquals(hostUser.getId(), leaveBroadcast.getSenderId());

        sessionB.disconnect();
    }
}
