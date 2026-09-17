package com.meetmind.meetmind_backend.representative;

import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
import com.meetmind.meetmind_backend.event.SpringMeetingEndedEvent;
import com.meetmind.meetmind_backend.event.SpringMeetingStartedEvent;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=update"
)
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AiRepresentativeIntegrationTest {

    @MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private AiRepresentativeRepository representativeRepository;
    @Autowired private RepresentativeReportRepository reportRepository;
    @Autowired private AiRepresentativeService representativeService;
    @Autowired private RepresentativeReportService reportService;
    @Autowired private RepresentativeMeetingEventListener meetingEventListener;
    @Autowired private JwtService jwtService;
    @Autowired private AiProxySpeechService speechService;

    private User host;
    private User participant;
    private User outsider;
    private Meeting meeting;
    private Meeting meeting2;
    private String hostToken;
    private String participantToken;
    private String outsiderToken;
    private Path validTempMediaFile;

    @BeforeEach
    void setUp() throws Exception {
        reset(messagingTemplate);

        reportRepository.deleteAll();
        representativeRepository.deleteAll();
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        host = new User();
        host.setName("Rep Host");
        host.setEmail("rep_host@test.com");
        host.setPassword("password");
        host = userRepository.save(host);

        participant = new User();
        participant.setName("Rep Participant");
        participant.setEmail("rep_participant@test.com");
        participant.setPassword("password");
        participant = userRepository.save(participant);

        outsider = new User();
        outsider.setName("Rep Outsider");
        outsider.setEmail("rep_outsider@test.com");
        outsider.setPassword("password");
        outsider = userRepository.save(outsider);

        meeting = new Meeting();
        meeting.setTitle("Representative Test Meeting 1");
        meeting.setHost(host);
        meeting.setStatus(MeetingStatus.LIVE);
        meeting.setScheduledAt(LocalDateTime.now().minusHours(1));
        meeting.setStartedAt(LocalDateTime.now());
        meeting = meetingRepository.save(meeting);

        meeting2 = new Meeting();
        meeting2.setTitle("Representative Test Meeting 2");
        meeting2.setHost(host);
        meeting2.setStatus(MeetingStatus.SCHEDULED);
        meeting2.setScheduledAt(LocalDateTime.now().plusHours(2));
        meeting2 = meetingRepository.save(meeting2);

        MeetingParticipant hostPart = new MeetingParticipant();
        hostPart.setMeeting(meeting);
        hostPart.setUser(host);
        hostPart.setRole(ParticipantRole.HOST);
        hostPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(hostPart);

        MeetingParticipant guestPart = new MeetingParticipant();
        guestPart.setMeeting(meeting);
        guestPart.setUser(participant);
        guestPart.setRole(ParticipantRole.PARTICIPANT);
        guestPart.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(guestPart);

        hostToken = jwtService.generateToken(host.getId(), host.getEmail());
        participantToken = jwtService.generateToken(participant.getId(), participant.getEmail());
        outsiderToken = jwtService.generateToken(outsider.getId(), outsider.getEmail());

        validTempMediaFile = Files.createTempFile("approved-intro-", ".mp4");
        Files.write(validTempMediaFile, new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70});
    }

    @AfterEach
    void tearDown() throws Exception {
        if (validTempMediaFile != null && Files.exists(validTempMediaFile)) {
            Files.delete(validTempMediaFile);
        }
    }

    @Test
    @Order(1)
    void testCreateRepresentative_Success() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "monitoredTopics", List.of("security", "budget"),
                "monitoredQuestions", List.of("When is launch?"),
                "importantPeople", List.of("Rep Host")
        );

        ResponseEntity<AiRepresentative> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/representatives",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                AiRepresentative.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOwnerId()).isEqualTo(participant.getId());
        assertThat(response.getBody().getMeetingId()).isEqualTo(meeting.getId());
        assertThat(response.getBody().getStatus()).isEqualTo(RepresentativeStatus.SCHEDULED);
        assertThat(response.getBody().getConsentDisclosure()).isTrue();
    }

    @Test
    @Order(2)
    void testUnauthorizedUser_CannotConfigureAnotherUserRepresentative() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(outsiderToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("monitoredTopics", List.of("confidential"));

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/representatives",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(3)
    void testRepresentativeActivatesOnlyForCorrectMeeting() {
        AiRepresentative rep1 = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("topic1"), List.of(), List.of(), null, null
        );

        AiRepresentative rep2 = representativeService.createRepresentative(
                meeting2.getId(), host, List.of("topic2"), List.of(), List.of(), null, null
        );

        // Activate meeting 1
        meetingEventListener.handleMeetingStarted(new SpringMeetingStartedEvent(
                this, meeting.getId(), host.getId(), host.getName(), meeting.getTitle()
        ));

        AiRepresentative updatedRep1 = representativeRepository.findById(rep1.getId()).orElseThrow();
        AiRepresentative updatedRep2 = representativeRepository.findById(rep2.getId()).orElseThrow();

        assertThat(updatedRep1.getStatus()).isEqualTo(RepresentativeStatus.ACTIVE);
        assertThat(updatedRep2.getStatus()).isEqualTo(RepresentativeStatus.SCHEDULED);
    }

    @Test
    @Order(4)
    void testInvalidMedia_Rejected() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("topic"), List.of(), List.of(), null, null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("mediaStoragePath", "/nonexistent/video.mp4");

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/representatives/" + rep.getId() + "/media",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(5)
    void testStateTransitions_CreatedToScheduledToActiveToCompleted() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("design"), List.of(), List.of(), null, null
        );
        assertThat(rep.getStatus()).isEqualTo(RepresentativeStatus.SCHEDULED);

        // Upload media
        representativeService.uploadMedia(meeting.getId(), rep.getId(), validTempMediaFile.toString(), participant);

        // Activate meeting
        meetingEventListener.handleMeetingStarted(new SpringMeetingStartedEvent(
                this, meeting.getId(), host.getId(), host.getName(), meeting.getTitle()
        ));
        AiRepresentative activeRep = representativeRepository.findById(rep.getId()).orElseThrow();
        assertThat(activeRep.getStatus()).isEqualTo(RepresentativeStatus.ACTIVE);

        // End meeting
        meetingEventListener.handleMeetingEnded(new SpringMeetingEndedEvent(
                this, meeting.getId(), host.getId(), host.getName()
        ));
        AiRepresentative completedRep = representativeRepository.findById(rep.getId()).orElseThrow();
        assertThat(completedRep.getStatus()).isEqualTo(RepresentativeStatus.COMPLETED);
    }

    @Test
    @Order(6)
    void testCancellation_Works() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("cancel-test"), List.of(), List.of(), null, null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);

        ResponseEntity<AiRepresentative> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/representatives/" + rep.getId() + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                AiRepresentative.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(RepresentativeStatus.CANCELLED);
    }

    @Test
    @Order(7)
    void testMeetingEndsBeforeActivation_HandlesGracefully() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("early-end"), List.of(), List.of(), null, null
        );

        // Meeting ends directly without start event
        meetingEventListener.handleMeetingEnded(new SpringMeetingEndedEvent(
                this, meeting.getId(), host.getId(), host.getName()
        ));

        AiRepresentative updated = representativeRepository.findById(rep.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RepresentativeStatus.COMPLETED);
    }

    @Test
    @Order(8)
    void testProcessingFailure_Handled() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("failure-test"), List.of(), List.of(), null, null
        );

        representativeService.failRepresentative(rep.getId(), "Simulated failure");

        AiRepresentative failedRep = representativeRepository.findById(rep.getId()).orElseThrow();
        assertThat(failedRep.getStatus()).isEqualTo(RepresentativeStatus.FAILED);
    }

    @Test
    @Order(9)
    void testPostMeetingReport_GeneratedWithMonitoredTopicsAndQuestions() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("architecture"), List.of("What is the release date?"), List.of("Rep Host"), null, null
        );

        representativeService.activateForMeeting(meeting.getId());
        representativeService.completeForMeeting(meeting.getId());

        RepresentativeReport report = reportRepository.findByRepresentativeId(rep.getId()).orElseThrow();
        assertThat(report).isNotNull();
        assertThat(report.getOwnerId()).isEqualTo(participant.getId());
        assertThat(report.getMonitoredTopicsFoundJson()).contains("architecture");
    }

    @Test
    @Order(10)
    void testAutomatedParticipant_VisiblyIdentifiedAndDisclosed() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("identity-test"), List.of(), List.of(), null, null
        );

        representativeService.activateForMeeting(meeting.getId());

        // Verify automated participant role
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meeting.getId());
        boolean hasAutomatedRole = participants.stream()
                .anyMatch(p -> p.getRole() == ParticipantRole.AUTOMATED_AGENT);

        assertThat(hasAutomatedRole).isTrue();

        // Verify STOMP disclosure message was sent
        verify(messagingTemplate).convertAndSend(
                eq("/topic/meetings/" + meeting.getId() + "/chat"),
                any(Object.class)
        );
    }

    @Test
    @Order(11)
    void testAiRepresentative_RealTimeVoiceGeneration_WhenMentionedOrTopicMatched() {
        reset(messagingTemplate);

        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("budget", "timeline"), List.of("What is the deadline?"), List.of("Rep Host"), null, null
        );

        representativeService.activateForMeeting(meeting.getId());

        // Simulate participant in meeting asking a question mentioning participant's name and monitored topic
        List<com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage> responses =
                speechService.handleIncomingChatMessage(meeting.getId(), host.getId(), host.getName(), "Hey @Rep Participant what is the budget status?");

        assertThat(responses).isNotEmpty();
        com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage speech = responses.get(0);
        assertThat(speech.meetingId()).isEqualTo(meeting.getId());
        assertThat(speech.ownerId()).isEqualTo(participant.getId());
        assertThat(speech.spokenText()).contains("budget");
        assertThat(speech.spokenText()).contains("Rep Participant");

        // Verify broadcast to the AI Proxy speech STOMP topic
        verify(messagingTemplate).convertAndSend(
                eq("/topic/meetings/" + meeting.getId() + "/ai-proxy/speech"),
                eq(speech)
        );
    }

    @Test
    @Order(12)
    void testAiRepresentative_DirectQuerySpeechEndpoint() {
        AiRepresentative rep = representativeService.createRepresentative(
                meeting.getId(), participant, List.of("architecture"), List.of(), List.of(), null, null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("query", "Where is the participant?"), headers);

        ResponseEntity<com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/representatives/" + rep.getId() + "/speak",
                request,
                com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().spokenText()).contains("Rep Participant");
    }
}
