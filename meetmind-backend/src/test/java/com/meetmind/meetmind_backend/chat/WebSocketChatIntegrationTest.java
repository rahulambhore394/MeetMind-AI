package com.meetmind.meetmind_backend.chat;

import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageRequest;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketChatIntegrationTest {

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

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private User hostUser;
    private User participantUser;
    private User nonParticipantUser;
    private Meeting meeting;
    private String hostToken;
    private String participantToken;
    private String nonParticipantToken;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        // Clean up database to avoid conflicts
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        // Create Users
        hostUser = new User();
        hostUser.setName("Host User");
        hostUser.setEmail("host@ws.com");
        hostUser.setPassword("password");
        hostUser = userRepository.save(hostUser);

        participantUser = new User();
        participantUser.setName("Participant User");
        participantUser.setEmail("participant@ws.com");
        participantUser.setPassword("password");
        participantUser = userRepository.save(participantUser);

        nonParticipantUser = new User();
        nonParticipantUser.setName("Non-Participant User");
        nonParticipantUser.setEmail("stranger@ws.com");
        nonParticipantUser.setPassword("password");
        nonParticipantUser = userRepository.save(nonParticipantUser);

        // Generate JWTs
        hostToken = jwtService.generateToken(hostUser.getId(), hostUser.getEmail());
        participantToken = jwtService.generateToken(participantUser.getId(), participantUser.getEmail());
        nonParticipantToken = jwtService.generateToken(nonParticipantUser.getId(), nonParticipantUser.getEmail());

        // Create Meeting
        meeting = new Meeting();
        meeting.setTitle("WS Chat Meeting");
        meeting.setDescription("Testing WebSockets");
        meeting.setHost(hostUser);
        meeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting = meetingRepository.save(meeting);

        // Add Host as Participant
        MeetingParticipant hostPart = new MeetingParticipant();
        hostPart.setMeeting(meeting);
        hostPart.setUser(hostUser);
        hostPart.setRole(ParticipantRole.HOST);
        hostPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(hostPart);

        // Add Participant User as Participant
        MeetingParticipant partPart = new MeetingParticipant();
        partPart.setMeeting(meeting);
        partPart.setUser(participantUser);
        partPart.setRole(ParticipantRole.PARTICIPANT);
        partPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(partPart);

        // Configure Stomp Client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
    void testWebSocketSendAndReceive_Success() throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + hostToken);

        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        System.out.println("CLIENT DEBUG EXCEPTION: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        System.out.println("CLIENT DEBUG TRANSPORT ERROR: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                }
        );

        StompSession session = sessionFuture.get(5, TimeUnit.SECONDS);
        assertTrue(session.isConnected());

        LinkedBlockingQueue<ChatMessageResponse> messageQueue = new LinkedBlockingQueue<>();

        session.subscribe("/topic/meetings/" + meeting.getId() + "/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageResponse) payload);
            }
        });

        // Wait for subscription to be registered on the broker to avoid race condition
        Thread.sleep(1000);

        // Send a message
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(meeting.getId());
        request.setMessage("Hello via WebSocket!");

        session.send("/app/chat.send", request);

        // Wait for message on topic
        ChatMessageResponse broadcast = messageQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(broadcast);
        assertEquals("Hello via WebSocket!", broadcast.getMessage());
        assertEquals("Host User", broadcast.getSenderName());
        assertEquals(hostUser.getId(), broadcast.getSenderId());

        session.disconnect();
    }

    @Test
    void testWebSocketSubscribe_ForbiddenForNonParticipant() throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + nonParticipantToken);

        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        );

        StompSession session = sessionFuture.get(5, TimeUnit.SECONDS);
        assertTrue(session.isConnected());

        LinkedBlockingQueue<ChatMessageResponse> messageQueue = new LinkedBlockingQueue<>();

        session.subscribe("/topic/meetings/" + meeting.getId() + "/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((ChatMessageResponse) payload);
            }
        });

        // Now, host sends a message
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMeetingId(meeting.getId());
        request.setMessage("Secret message!");

        // Connect host to send the message
        StompHeaders hostConnectHeaders = new StompHeaders();
        hostConnectHeaders.add("Authorization", "Bearer " + hostToken);
        StompSession hostSession = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                hostConnectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        hostSession.send("/app/chat.send", request);

        // Verify that the non-participant did NOT receive the message (silent drop of subscription)
        ChatMessageResponse broadcast = messageQueue.poll(2, TimeUnit.SECONDS);
        assertNull(broadcast);

        session.disconnect();
        hostSession.disconnect();
    }
}
