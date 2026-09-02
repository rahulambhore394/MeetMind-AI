package com.meetmind.meetmind_backend.recording;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=update"
)
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
public class RecordingIntegrationTest {

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
    private RecordingRepository recordingRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    private User hostUser;
    private User participantUser;
    private User strangerUser;

    private Meeting liveMeeting;
    private Meeting scheduledMeeting;

    private String hostToken;
    private String participantToken;
    private String strangerToken;

    @BeforeEach
    void setUp() {
        recordingRepository.deleteAll();
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        // Users
        hostUser = new User();
        hostUser.setName("Host User");
        hostUser.setEmail("host@recording.com");
        hostUser.setPassword("password");
        hostUser = userRepository.save(hostUser);

        participantUser = new User();
        participantUser.setName("Participant User");
        participantUser.setEmail("participant@recording.com");
        participantUser.setPassword("password");
        participantUser = userRepository.save(participantUser);

        strangerUser = new User();
        strangerUser.setName("Stranger User");
        strangerUser.setEmail("stranger@recording.com");
        strangerUser.setPassword("password");
        strangerUser = userRepository.save(strangerUser);

        // Tokens
        hostToken = jwtService.generateToken(hostUser.getId(), hostUser.getEmail());
        participantToken = jwtService.generateToken(participantUser.getId(), participantUser.getEmail());
        strangerToken = jwtService.generateToken(strangerUser.getId(), strangerUser.getEmail());

        // Live Meeting
        liveMeeting = new Meeting();
        liveMeeting.setTitle("Live Meeting Sync");
        liveMeeting.setDescription("A live session");
        liveMeeting.setHost(hostUser);
        liveMeeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        liveMeeting.setStatus(MeetingStatus.LIVE);
        liveMeeting = meetingRepository.save(liveMeeting);

        // Scheduled Meeting
        scheduledMeeting = new Meeting();
        scheduledMeeting.setTitle("Scheduled Meeting Sync");
        scheduledMeeting.setHost(hostUser);
        scheduledMeeting.setScheduledAt(LocalDateTime.now().plusDays(2));
        scheduledMeeting.setStatus(MeetingStatus.SCHEDULED);
        scheduledMeeting = meetingRepository.save(scheduledMeeting);

        // Host Participant
        MeetingParticipant hostPart = new MeetingParticipant();
        hostPart.setMeeting(liveMeeting);
        hostPart.setUser(hostUser);
        hostPart.setRole(ParticipantRole.HOST);
        hostPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(hostPart);

        // Attendee Participant
        MeetingParticipant partPart = new MeetingParticipant();
        partPart.setMeeting(liveMeeting);
        partPart.setUser(participantUser);
        partPart.setRole(ParticipantRole.PARTICIPANT);
        partPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(partPart);
    }

    @AfterEach
    void tearDown() {
        recordingRepository.deleteAll();
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testStartRecording_Unauthenticated_Returns403() {
        ResponseEntity<Object> response = restTemplate.postForEntity(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/start",
                null,
                Object.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testStartRecording_Success_AsHost() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<MeetingRecording> response = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/start",
                HttpMethod.POST,
                entity,
                MeetingRecording.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MeetingRecording body = response.getBody();
        assertNotNull(body);
        assertEquals(liveMeeting.getId(), body.getMeetingId());
        assertEquals(hostUser.getId(), body.getOwnerId());
        assertEquals(RecordingStatus.STARTED, body.getStatus());
        assertNotNull(body.getStartedAt());
    }

    @Test
    void testStartRecording_Forbidden_AsParticipant() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/start",
                HttpMethod.POST,
                entity,
                Object.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testStartRecording_BadRequest_ScheduledMeeting() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/meetings/" + scheduledMeeting.getId() + "/recordings/start",
                HttpMethod.POST,
                entity,
                Object.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testStartRecording_Conflict_RepeatedStart() {
        // Pre-create a started recording
        MeetingRecording preRecording = new MeetingRecording();
        preRecording.setMeetingId(liveMeeting.getId());
        preRecording.setOwnerId(hostUser.getId());
        preRecording.setStartedAt(LocalDateTime.now());
        preRecording.setStatus(RecordingStatus.STARTED);
        recordingRepository.save(preRecording);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/start",
                HttpMethod.POST,
                entity,
                Object.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testFullRecordingLifecycle_Success() {
        // 1. Start Recording
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<MeetingRecording> startRes = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/start",
                HttpMethod.POST,
                entity,
                MeetingRecording.class
        );
        assertEquals(HttpStatus.OK, startRes.getStatusCode());
        MeetingRecording recording = startRes.getBody();
        assertNotNull(recording);

        // 2. Stop Recording
        ResponseEntity<MeetingRecording> stopRes = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/" + recording.getId() + "/stop",
                HttpMethod.POST,
                entity,
                MeetingRecording.class
        );
        assertEquals(HttpStatus.OK, stopRes.getStatusCode());
        MeetingRecording stopped = stopRes.getBody();
        assertNotNull(stopped);
        assertEquals(RecordingStatus.PROCESSING, stopped.getStatus());
        assertNotNull(stopped.getEndedAt());
        assertNotNull(stopped.getDuration());

        // 3. Upload File
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setBearerAuth(hostToken);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource("Recording byte data".getBytes()) {
            @Override
            public String getFilename() {
                return "test-recording.mp4";
            }
        };
        body.add("file", fileResource);
        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(body, uploadHeaders);

        ResponseEntity<MeetingRecording> uploadRes = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/" + recording.getId() + "/upload",
                HttpMethod.POST,
                uploadEntity,
                MeetingRecording.class
        );
        assertEquals(HttpStatus.OK, uploadRes.getStatusCode());
        MeetingRecording completed = uploadRes.getBody();
        assertNotNull(completed);
        assertEquals(RecordingStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getStoragePath());

        // 4. Download File as Participant
        HttpHeaders downloadHeaders = new HttpHeaders();
        downloadHeaders.setBearerAuth(participantToken);
        HttpEntity<Void> downloadEntity = new HttpEntity<>(downloadHeaders);

        ResponseEntity<byte[]> downloadRes = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/" + recording.getId() + "/download",
                HttpMethod.GET,
                downloadEntity,
                byte[].class
        );
        assertEquals(HttpStatus.OK, downloadRes.getStatusCode());
        assertNotNull(downloadRes.getBody());
        assertEquals("Recording byte data", new String(downloadRes.getBody()));

        // 5. Download File as Stranger (Blocked)
        HttpHeaders strangerHeaders = new HttpHeaders();
        strangerHeaders.setBearerAuth(strangerToken);
        HttpEntity<Void> strangerEntity = new HttpEntity<>(strangerHeaders);

        ResponseEntity<Object> blockedRes = restTemplate.exchange(
                "/api/meetings/" + liveMeeting.getId() + "/recordings/" + recording.getId() + "/download",
                HttpMethod.GET,
                strangerEntity,
                Object.class
        );
        assertEquals(HttpStatus.FORBIDDEN, blockedRes.getStatusCode());
    }
}
