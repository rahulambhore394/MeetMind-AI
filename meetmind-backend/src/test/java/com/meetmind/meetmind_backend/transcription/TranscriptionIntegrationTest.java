package com.meetmind.meetmind_backend.transcription;

import com.meetmind.meetmind_backend.auth.jwt.JwtService;
import com.meetmind.meetmind_backend.chat.TestRedisConfig;
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
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Phase 15: AI Transcription.
 *
 * Uses a MOCK TranscriptionProvider — no Vosk model download required.
 * The KafkaEventPublisher is mocked to avoid requiring a running Kafka broker.
 *
 * Mock provider behavior (driven by language param):
 *   "en"         → 2 timestamped segments (success)
 *   "hi"         → 0 segments (empty result, still COMPLETED)
 *   "FAIL"       → throws TranscriptionException (simulated failure)
 *   "UNSUPPORTED"→ throws TranscriptionException (unsupported language)
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=update"
)
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TranscriptionIntegrationTest {

    // ── Mock beans ────────────────────────────────────────────────────────────

    @MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    @TestConfiguration
    static class MockProviderConfig {
        @Bean
        @Primary
        public TranscriptionProvider mockTranscriptionProvider() {
            return new TranscriptionProvider() {
                @Override
                public String providerName() { return "mock"; }

                @Override
                public boolean supportsLanguage(String language) {
                    return !"UNSUPPORTED".equals(language);
                }

                @Override
                public TranscriptionResult transcribe(Path audioPath, String language) throws TranscriptionException {
                    if ("FAIL".equals(language)) {
                        throw new TranscriptionException("Simulated provider failure for testing");
                    }
                    if ("UNSUPPORTED".equals(language)) {
                        throw new TranscriptionException("Unsupported language: UNSUPPORTED");
                    }
                    if ("hi".equals(language)) {
                        return new TranscriptionResult(List.of());
                    }
                    return new TranscriptionResult(List.of(
                            new TranscriptionResult.SegmentData("Hello this is a meeting", 0L, 2500L, null, 0.95),
                            new TranscriptionResult.SegmentData("The discussion is about the quarterly report", 2600L, 6000L, null, 0.88)
                    ));
                }
            };
        }
    }

    // ── Injected beans ────────────────────────────────────────────────────────

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private RecordingRepository recordingRepository;
    @Autowired private TranscriptRepository transcriptRepository;
    @Autowired private TranscriptSegmentRepository segmentRepository;
    @Autowired private TranscriptionService transcriptionService;
    @Autowired private JwtService jwtService;

    // ── Test state ────────────────────────────────────────────────────────────

    private User host;
    private User participant;
    private User outsider;
    private Meeting meeting;
    private String hostToken;
    private String participantToken;
    private String outsiderToken;
    private Path tempAudioFile;

    @BeforeEach
    void setUp() throws Exception {
        segmentRepository.deleteAll();
        transcriptRepository.deleteAll();
        recordingRepository.deleteAll();
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        host = new User();
        host.setName("Host User");
        host.setEmail("host_transcript@test.com");
        host.setPassword("password");
        host = userRepository.save(host);

        participant = new User();
        participant.setName("Participant User");
        participant.setEmail("participant_transcript@test.com");
        participant.setPassword("password");
        participant = userRepository.save(participant);

        outsider = new User();
        outsider.setName("Outsider User");
        outsider.setEmail("outsider_transcript@test.com");
        outsider.setPassword("password");
        outsider = userRepository.save(outsider);

        meeting = new Meeting();
        meeting.setTitle("Transcript Test Meeting");
        meeting.setHost(host);
        meeting.setStatus(MeetingStatus.LIVE);
        meeting.setScheduledAt(LocalDateTime.now().minusHours(1));
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

        hostToken = jwtService.generateToken(host.getId(), host.getEmail());
        participantToken = jwtService.generateToken(participant.getId(), participant.getEmail());
        outsiderToken = jwtService.generateToken(outsider.getId(), outsider.getEmail());

        // Create a non-empty temp file simulating an uploaded audio file
        tempAudioFile = Files.createTempFile("test-audio-", ".wav");
        Files.write(tempAudioFile, new byte[]{0x52, 0x49, 0x46, 0x46, 0x00}); // 5 bytes, non-empty
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempAudioFile != null && Files.exists(tempAudioFile)) {
            Files.delete(tempAudioFile);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MeetingRecording createCompletedRecording(String storagePath) {
        MeetingRecording r = new MeetingRecording();
        r.setMeetingId(meeting.getId());
        r.setOwnerId(host.getId());
        r.setStartedAt(LocalDateTime.now().minusMinutes(10));
        r.setEndedAt(LocalDateTime.now().minusMinutes(1));
        r.setDuration(540L);
        r.setStoragePath(storagePath);
        r.setStatus(RecordingStatus.COMPLETED);
        return recordingRepository.save(r);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testTranscriptionService_Success() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());

        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), tempAudioFile.toString(), "en"
        );

        assertThat(transcript.getStatus()).isEqualTo(TranscriptionStatus.COMPLETED);
        assertThat(transcript.getFullText()).contains("Hello this is a meeting");
        assertThat(transcript.getProviderName()).isEqualTo("mock");
        assertThat(transcript.getCompletedAt()).isNotNull();
        assertThat(transcript.getLanguage()).isEqualTo("en");

        List<TranscriptSegment> segments = segmentRepository.findByTranscriptIdOrderBySegmentIndex(transcript.getId());
        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).getStartMs()).isEqualTo(0L);
        assertThat(segments.get(0).getEndMs()).isEqualTo(2500L);
        assertThat(segments.get(0).getConfidence()).isEqualTo(0.95);
        assertThat(segments.get(1).getText()).contains("quarterly");
    }

    @Test
    @Order(2)
    void testTranscriptionService_EmptyResult_StillCompleted() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());

        // "hi" → mock returns empty List
        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), tempAudioFile.toString(), "hi"
        );

        assertThat(transcript.getStatus()).isEqualTo(TranscriptionStatus.COMPLETED);

        List<TranscriptSegment> segments = segmentRepository.findByTranscriptIdOrderBySegmentIndex(transcript.getId());
        assertThat(segments).isEmpty();
    }

    @Test
    @Order(3)
    void testTranscriptionService_ProviderFailure_ProducesFailedStatus() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());

        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), tempAudioFile.toString(), "FAIL"
        );

        assertThat(transcript.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(transcript.getErrorMessage()).contains("Simulated provider failure");
        assertThat(transcript.getCompletedAt()).isNotNull();
    }

    @Test
    @Order(4)
    void testTranscriptionService_MissingFile_ProducesFailedStatus() {
        MeetingRecording recording = createCompletedRecording("/nonexistent/path/audio.wav");

        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), "/nonexistent/path/audio.wav", "en"
        );

        assertThat(transcript.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(transcript.getErrorMessage()).contains("not found on disk");
    }

    @Test
    @Order(5)
    void testTranscriptionService_NullStoragePath_ProducesFailedStatus() {
        MeetingRecording recording = createCompletedRecording(null);

        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), null, "en"
        );

        assertThat(transcript.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(transcript.getErrorMessage()).contains("null or empty");
    }

    @Test
    @Order(6)
    void testTranscriptionService_Idempotency_SkipsIfAlreadyCompleted() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());

        MeetingTranscript first = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), tempAudioFile.toString(), "en"
        );
        assertThat(first.getStatus()).isEqualTo(TranscriptionStatus.COMPLETED);

        // Second call — should be swallowed by idempotency guard
        try {
            transcriptionService.transcribe(
                    recording.getId(), meeting.getId(), tempAudioFile.toString(), "en"
            );
        } catch (TranscriptionService.AlreadyTranscribedException ignored) {
            // expected
        }

        // Exactly one transcript should exist
        assertThat(transcriptRepository.findByMeetingId(meeting.getId())).hasSize(1);
    }

    @Test
    @Order(7)
    void testGetTranscripts_AsParticipant_Returns200() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());
        transcriptionService.transcribe(recording.getId(), meeting.getId(), tempAudioFile.toString(), "en");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);

        ResponseEntity<List> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/transcripts",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Order(8)
    void testGetTranscripts_AsOutsider_Returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(outsiderToken);

        ResponseEntity<Object> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/transcripts",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(9)
    void testGetTranscriptDetail_IncludesSegments() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());
        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), tempAudioFile.toString(), "en"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId()
                        + "/transcripts/" + transcript.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("transcript");
        assertThat(response.getBody()).containsKey("segments");
    }

    @Test
    @Order(10)
    void testTriggerTranscription_ByHost_Returns200() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hostToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("recordingId", recording.getId(), "language", "en");
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/transcripts/trigger",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(11)
    void testTriggerTranscription_ByNonHost_Returns403() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("recordingId", recording.getId(), "language", "en");
        ResponseEntity<Object> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/transcripts/trigger",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(12)
    void testTranscriptPersistence_AllMetadataFields() {
        MeetingRecording recording = createCompletedRecording(tempAudioFile.toString());
        MeetingTranscript transcript = transcriptionService.transcribe(
                recording.getId(), meeting.getId(), tempAudioFile.toString(), "en"
        );

        // Re-read from DB to verify persistence
        MeetingTranscript reloaded = transcriptRepository.findById(transcript.getId()).orElseThrow();
        assertThat(reloaded.getMeetingId()).isEqualTo(meeting.getId());
        assertThat(reloaded.getRecordingId()).isEqualTo(recording.getId());
        assertThat(reloaded.getLanguage()).isEqualTo("en");
        assertThat(reloaded.getProviderName()).isEqualTo("mock");
        assertThat(reloaded.getStatus()).isEqualTo(TranscriptionStatus.COMPLETED);
        assertThat(reloaded.getFullText()).isNotBlank();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getCompletedAt()).isNotNull();
        assertThat(reloaded.getErrorMessage()).isNull();
    }
}
