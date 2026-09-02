package com.meetmind.meetmind_backend.intelligence;

import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
import com.meetmind.meetmind_backend.event.EventIdempotencyRegistry;
import com.meetmind.meetmind_backend.event.MeetMindEvent;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRole;
import com.meetmind.meetmind_backend.participant.ParticipantStatus;
import com.meetmind.meetmind_backend.recording.MeetingRecording;
import com.meetmind.meetmind_backend.recording.RecordingRepository;
import com.meetmind.meetmind_backend.recording.RecordingStatus;
import com.meetmind.meetmind_backend.transcription.*;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=update"
)
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MeetingIntelligenceIntegrationTest {

    @MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private RecordingRepository recordingRepository;
    @Autowired private TranscriptRepository transcriptRepository;
    @Autowired private TranscriptSegmentRepository segmentRepository;
    @Autowired private MeetingSummaryRepository summaryRepository;
    @Autowired private ActionItemRepository actionItemRepository;
    @Autowired private MeetingIntelligenceService intelligenceService;
    @Autowired private IntelligenceConsumer intelligenceConsumer;
    @Autowired private EventIdempotencyRegistry idempotencyRegistry;
    @Autowired private JwtService jwtService;

    private User host;
    private User participant;
    private User outsider;
    private Meeting meeting;
    private MeetingRecording recording;
    private String hostToken;
    private String participantToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        actionItemRepository.deleteAll();
        summaryRepository.deleteAll();
        segmentRepository.deleteAll();
        transcriptRepository.deleteAll();
        recordingRepository.deleteAll();
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        host = new User();
        host.setName("Intel Host");
        host.setEmail("intel_host@test.com");
        host.setPassword("password");
        host = userRepository.save(host);

        participant = new User();
        participant.setName("Intel Participant");
        participant.setEmail("intel_participant@test.com");
        participant.setPassword("password");
        participant = userRepository.save(participant);

        outsider = new User();
        outsider.setName("Intel Outsider");
        outsider.setEmail("intel_outsider@test.com");
        outsider.setPassword("password");
        outsider = userRepository.save(outsider);

        meeting = new Meeting();
        meeting.setTitle("Q3 Strategy & Release Planning");
        meeting.setHost(host);
        meeting.setStatus(MeetingStatus.LIVE);
        meeting.setScheduledAt(LocalDateTime.now().minusHours(1));
        meeting.setStartedAt(LocalDateTime.now().minusMinutes(45));
        meeting = meetingRepository.save(meeting);

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

        recording = new MeetingRecording();
        recording.setMeetingId(meeting.getId());
        recording.setOwnerId(host.getId());
        recording.setStartedAt(LocalDateTime.now().minusMinutes(40));
        recording.setEndedAt(LocalDateTime.now().minusMinutes(5));
        recording.setDuration(2100L);
        recording.setStoragePath("/tmp/test_intel.mp4");
        recording.setStatus(RecordingStatus.COMPLETED);
        recording = recordingRepository.save(recording);

        hostToken = jwtService.generateToken(host.getId(), host.getEmail());
        participantToken = jwtService.generateToken(participant.getId(), participant.getEmail());
        outsiderToken = jwtService.generateToken(outsider.getId(), outsider.getEmail());
    }

    private MeetingTranscript createTranscript(String fullText) {
        MeetingTranscript t = new MeetingTranscript();
        t.setMeetingId(meeting.getId());
        t.setRecordingId(recording.getId());
        t.setLanguage("en");
        t.setStatus(TranscriptionStatus.COMPLETED);
        t.setFullText(fullText);
        t.setProviderName("vosk");
        t.setCompletedAt(LocalDateTime.now());
        return transcriptRepository.save(t);
    }

    @Test
    @Order(1)
    void testNormalTranscript_GeneratesSummaryAndActionItems() throws Exception {
        String transcriptText = "Welcome everyone to the quarterly review. We agreed that the new feature launch will be postponed. " +
                "Alice will complete the security audit by Friday. " +
                "Bob is assigned to update the API documentation before end of day. " +
                "What is the timeline for mobile release? We decided to use Postgres for database.";

        MeetingTranscript transcript = createTranscript(transcriptText);

        TranscriptSegment s1 = new TranscriptSegment();
        s1.setTranscriptId(transcript.getId());
        s1.setSegmentIndex(0);
        s1.setText("Welcome everyone to the quarterly review. We agreed that the new feature launch will be postponed.");
        s1.setSpeaker("Alice");
        s1.setStartMs(0L);
        s1.setEndMs(5000L);
        segmentRepository.save(s1);

        TranscriptSegment s2 = new TranscriptSegment();
        s2.setTranscriptId(transcript.getId());
        s2.setSegmentIndex(1);
        s2.setText("Alice will complete the security audit by Friday. Bob is assigned to update the API documentation before end of day.");
        s2.setSpeaker("Bob");
        s2.setStartMs(5500L);
        s2.setEndMs(12000L);
        segmentRepository.save(s2);

        MeetingSummary summary = intelligenceService.generateIntelligence(transcript.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.getSummary()).isNotBlank();
        assertThat(summary.getDecisionsJson()).contains("We agreed that the new feature launch will be postponed");
        assertThat(summary.getQuestionsJson()).contains("What is the timeline for mobile release?");

        List<ActionItem> actionItems = actionItemRepository.findByMeetingSummaryId(summary.getId());
        assertThat(actionItems).hasSizeGreaterThanOrEqualTo(1);

        ActionItem item = actionItems.get(0);
        assertThat(item.getDescription()).isNotBlank();
        assertThat(item.getConfidence()).isGreaterThan(0.0);
        assertThat(item.getStatus()).isEqualTo(ActionItemStatus.OPEN);
    }

    @Test
    @Order(2)
    void testEmptyTranscript_HandlesGracefully() throws Exception {
        MeetingTranscript transcript = createTranscript("");

        MeetingSummary summary = intelligenceService.generateIntelligence(transcript.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.getSummary()).contains("No transcript content provided");
        assertThat(summary.getKeyPointsJson()).isEqualTo("[]");
        assertThat(summary.getDecisionsJson()).isEqualTo("[]");

        List<ActionItem> actionItems = actionItemRepository.findByMeetingSummaryId(summary.getId());
        assertThat(actionItems).isEmpty();
    }

    @Test
    @Order(3)
    void testShortTranscript_GeneratesMinimalIntelligence() throws Exception {
        MeetingTranscript transcript = createTranscript("Brief sync meeting.");

        MeetingSummary summary = intelligenceService.generateIntelligence(transcript.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.getSummary()).contains("Brief sync meeting.");
    }

    @Test
    @Order(4)
    void testLongTranscript_GeneratesComprehensiveAnalysis() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Discussion item ").append(i).append(" about project architecture and design patterns. ");
        }
        sb.append("We agreed to adopt clean architecture. Charlie will prepare the slide deck by Monday.");

        MeetingTranscript transcript = createTranscript(sb.toString());

        MeetingSummary summary = intelligenceService.generateIntelligence(transcript.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.getSummary()).isNotBlank();
        assertThat(summary.getAnalysisJson()).contains("wordCount");

        List<ActionItem> actionItems = actionItemRepository.findByMeetingSummaryId(summary.getId());
        assertThat(actionItems).isNotEmpty();
    }

    @Test
    @Order(5)
    void testAIProviderFailure_HandlesExceptionGracefully() {
        MeetingTranscript transcript = createTranscript("FAIL_PROVIDER trigger error test");

        assertThatThrownBy(() -> intelligenceService.generateIntelligence(transcript.getId()))
                .isInstanceOf(IntelligenceException.class)
                .hasMessageContaining("Simulated AI provider failure");
    }

    @Test
    @Order(6)
    void testMalformedTranscript_FallbackHandling() throws Exception {
        MeetingTranscript transcript = createTranscript("  some text with irregular   spacing... ");

        MeetingSummary summary = intelligenceService.generateIntelligence(transcript.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.getSummary()).isNotBlank();
    }

    @Test
    @Order(7)
    void testPersistence_SummaryAndActionItemsStored() throws Exception {
        MeetingTranscript transcript = createTranscript("We decided to proceed. Dave will deploy to staging by Friday.");

        MeetingSummary summary = intelligenceService.generateIntelligence(transcript.getId());

        assertThat(summaryRepository.findById(summary.getId())).isPresent();
        List<ActionItem> items = actionItemRepository.findByMeetingId(meeting.getId());
        assertThat(items).isNotEmpty();
    }

    @Test
    @Order(8)
    void testKafkaConsumer_TriggersIntelligenceOnTranscriptionCompleted() throws Exception {
        MeetingTranscript transcript = createTranscript("Kafka test text. We agreed on deployment. Eve assigned to test by tomorrow.");

        MeetMindEvent event = new MeetMindEvent(
                UUID.randomUUID().toString(),
                "TRANSCRIPTION_COMPLETED",
                System.currentTimeMillis(),
                1,
                meeting.getId(),
                host.getId(),
                Map.of(
                        "transcriptId", transcript.getId(),
                        "recordingId", recording.getId(),
                        "success", true
                )
        );

        intelligenceConsumer.consume(event);

        List<MeetingSummary> processed = intelligenceConsumer.getProcessedSummaries();
        assertThat(processed).isNotEmpty();
        assertThat(processed.get(0).getTranscriptId()).isEqualTo(transcript.getId());
    }

    @Test
    @Order(9)
    void testRestEndpoint_GetSummaryAndActionItems() throws Exception {
        MeetingTranscript transcript = createTranscript("REST API test. We decided on architecture. Frank will review by EOD.");
        intelligenceService.generateIntelligence(transcript.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/intelligence",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("meetingId", meeting.getId().intValue());
        assertThat(response.getBody()).containsKey("summary");

        ResponseEntity<List> actionItemsResp = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/action-items",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertThat(actionItemsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actionItemsResp.getBody()).isNotEmpty();
    }

    @Test
    @Order(10)
    void testRestEndpoint_UnauthorizedAccessForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(outsiderToken);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/intelligence",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
