package com.meetmind.meetmind_backend.translation;

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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Phase 16: Live Translation.
 *
 * Uses a MOCK TranslationProvider and mocked SimpMessagingTemplate.
 * No external services required.
 *
 * Manual test scenario documented at the bottom of this file.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=update"
)
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TranslationIntegrationTest {

    // ── Mock beans ────────────────────────────────────────────────────────────

    @MockBean
    private com.meetmind.meetmind_backend.event.KafkaEventPublisher kafkaEventPublisher;

    @TestConfiguration
    static class MockTranslationProviderConfig {

        @Bean
        @Primary
        public TranslationProvider mockTranslationProvider() {
            return new TranslationProvider() {
                @Override
                public String providerName() { return "mock-translation"; }

                @Override
                public boolean supportsLanguagePair(String src, String tgt) {
                    if (src == null || tgt == null) return false;
                    var supported = java.util.Set.of("en", "hi", "mr");
                    return supported.contains(src.toLowerCase()) && supported.contains(tgt.toLowerCase());
                }

                @Override
                public TranslationResult translate(String text, String src, String tgt) throws TranslationException {
                    if ("FAIL_LANG".equalsIgnoreCase(src)) {
                        throw new TranslationException("Simulated provider failure");
                    }
                    if (!supportsLanguagePair(src, tgt)) {
                        throw new UnsupportedLanguageException("Unsupported pair: " + src + " -> " + tgt);
                    }
                    if (src.equalsIgnoreCase(tgt)) {
                        return new TranslationResult(text, text, src, tgt, providerName());
                    }
                    // Simple mock translations
                    String translated = switch (src.toLowerCase() + "->" + tgt.toLowerCase()) {
                        case "hi->en" -> "Hello, let's start the meeting";
                        case "en->hi" -> "नमस्ते, मीटिंग शुरू करते हैं";
                        case "en->mr" -> "नमस्कार, बैठक सुरू करूया";
                        case "mr->en" -> "Hello, let's begin";
                        case "hi->mr" -> "नमस्कार, बैठक सुरू करूया";
                        case "mr->hi" -> "नमस्ते, मीटिंग शुरू करते हैं";
                        default -> text + " [translated]";
                    };
                    return new TranslationResult(text, translated, src, tgt, providerName());
                }
            };
        }

        @Bean
        @Primary
        public SimpMessagingTemplate mockMessagingTemplate() {
            return mock(SimpMessagingTemplate.class);
        }
    }

    // ── Injected beans ────────────────────────────────────────────────────────

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private LiveTranslationRepository translationRepository;
    @Autowired private LiveTranslationService translationService;
    @Autowired private TranslationPreferenceService preferenceService;
    @Autowired private TranslationProvider mockProvider;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private JwtService jwtService;

    // ── Test state ────────────────────────────────────────────────────────────

    private User host;
    private User participant;
    private User outsider;
    private Meeting meeting;
    private String hostToken;
    private String participantToken;
    private String outsiderToken;

    @BeforeEach
    void setUp() {
        reset(messagingTemplate);

        // Cleanup order matters for FK constraints
        translationRepository.deleteAll();
        participantRepository.deleteAll();
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        host = createUser("host-trans-" + System.nanoTime() + "@test.com", "Host Trans");
        participant = createUser("participant-trans-" + System.nanoTime() + "@test.com", "Participant Trans");
        outsider = createUser("outsider-trans-" + System.nanoTime() + "@test.com", "Outsider Trans");

        meeting = new Meeting();
        meeting.setTitle("Translation Test Meeting");
        meeting.setHost(host);
        meeting.setStatus(MeetingStatus.LIVE);
        meeting.setScheduledAt(java.time.LocalDateTime.now().minusHours(1));
        meeting.setStartedAt(java.time.LocalDateTime.now());
        meeting = meetingRepository.save(meeting);

        MeetingParticipant mp = new MeetingParticipant();
        mp.setMeeting(meeting);
        mp.setUser(host);
        mp.setRole(ParticipantRole.HOST);
        mp.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(mp);

        MeetingParticipant pp = new MeetingParticipant();
        pp.setMeeting(meeting);
        pp.setUser(participant);
        pp.setRole(ParticipantRole.PARTICIPANT);
        pp.setStatus(ParticipantStatus.ACCEPTED);
        participantRepository.save(pp);

        hostToken = jwtService.generateToken(host.getId(), host.getEmail());
        participantToken = jwtService.generateToken(participant.getId(), participant.getEmail());
        outsiderToken = jwtService.generateToken(outsider.getId(), outsider.getEmail());
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testSuccessfulTranslation_HindiToEnglish() throws TranslationException {
        String sourceText = "नमस्ते, मीटिंग शुरू करते हैं";

        LiveTranslation saved = translationService.translateAndBroadcast(
                meeting.getId(), null, sourceText, "hi", "en", "User A", System.currentTimeMillis()
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMeetingId()).isEqualTo(meeting.getId());
        assertThat(saved.getSourceLanguage()).isEqualTo("hi");
        assertThat(saved.getTargetLanguage()).isEqualTo("en");
        assertThat(saved.getSourceText()).isEqualTo(sourceText);
        assertThat(saved.getTranslatedText()).isNotBlank();
        assertThat(saved.getTranslatedText()).isEqualTo("Hello, let's start the meeting");
        assertThat(saved.getProviderName()).isEqualTo("mock-translation");

        // Verify persisted
        assertThat(translationRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @Order(2)
    void testSuccessfulTranslation_EnglishToHindi() throws TranslationException {
        String sourceText = "Can you hear me";

        LiveTranslation saved = translationService.translateAndBroadcast(
                meeting.getId(), null, sourceText, "en", "hi", "User A", System.currentTimeMillis()
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getSourceLanguage()).isEqualTo("en");
        assertThat(saved.getTargetLanguage()).isEqualTo("hi");
        assertThat(saved.getTranslatedText()).isNotBlank();
    }

    @Test
    @Order(3)
    void testSuccessfulTranslation_EnglishToMarathi() throws TranslationException {
        String sourceText = "Hello, let's start the meeting";

        LiveTranslation saved = translationService.translateAndBroadcast(
                meeting.getId(), null, sourceText, "en", "mr", "User A", System.currentTimeMillis()
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getSourceLanguage()).isEqualTo("en");
        assertThat(saved.getTargetLanguage()).isEqualTo("mr");
        assertThat(saved.getTranslatedText()).isNotBlank();
    }

    @Test
    @Order(4)
    void testSameSourceAndTargetLanguage_PassThrough() throws TranslationException {
        String sourceText = "Let's discuss the project status";

        LiveTranslation saved = translationService.translateAndBroadcast(
                meeting.getId(), null, sourceText, "en", "en", "User A", System.currentTimeMillis()
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getTranslatedText()).isEqualTo(sourceText);
        assertThat(saved.getSourceLanguage()).isEqualTo("en");
        assertThat(saved.getTargetLanguage()).isEqualTo("en");
    }

    @Test
    @Order(5)
    void testUnsupportedLanguage_ThrowsException() {
        assertThatThrownBy(() ->
                translationService.translateAndBroadcast(
                        meeting.getId(), null, "Some text", "fr", "de", "User A", null
                )
        ).isInstanceOf(TranslationException.class)
         .hasMessageContaining("Unsupported");
    }

    @Test
    @Order(6)
    void testProviderFailure_HandlesGracefully() {
        assertThatThrownBy(() ->
                translationService.translateAndBroadcast(
                        meeting.getId(), null, "text", "FAIL_LANG", "en", "User A", null
                )
        ).isInstanceOf(TranslationException.class)
         .hasMessageContaining("Simulated");
    }

    @Test
    @Order(7)
    void testDuplicateProcessing_SkippedForSameSegmentAndLanguage() throws TranslationException {
        Long segmentId = 999L;

        // First translation
        LiveTranslation first = translationService.translateAndBroadcast(
                meeting.getId(), segmentId, "نमस्ते", "hi", "en", "User A", System.currentTimeMillis()
        );
        assertThat(first).isNotNull();

        // Second call for same segmentId + targetLanguage should skip
        LiveTranslation second = translationService.translateAndBroadcast(
                meeting.getId(), segmentId, "नमस्ते again", "hi", "en", "User A", System.currentTimeMillis()
        );

        // Should return the existing entry without creating a new record
        assertThat(second).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(translationRepository.findByTranscriptSegmentId(segmentId)).hasSize(1);
    }

    @Test
    @Order(8)
    void testWebSocketDelivery_BroadcastsToCorrectTopic() throws TranslationException {
        String sourceText = "Good morning everyone";

        translationService.translateAndBroadcast(
                meeting.getId(), null, sourceText, "en", "hi", "User A", System.currentTimeMillis()
        );

        String expectedTopic = "/topic/meetings/" + meeting.getId() + "/translations/hi";
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedTopic), any(Object.class));
    }

    @Test
    @Order(9)
    void testWebSocketDelivery_DoesNotBroadcastToOtherLanguageTopics() throws TranslationException {
        translationService.translateAndBroadcast(
                meeting.getId(), null, "The meeting is ended", "en", "hi", "Host", System.currentTimeMillis()
        );

        // Should NOT broadcast to mr or en topics
        verify(messagingTemplate, never()).convertAndSend(
                contains("/translations/mr"), any(Object.class));
        verify(messagingTemplate, never()).convertAndSend(
                contains("/translations/en"), any(Object.class));
    }

    @Test
    @Order(10)
    void testUserLanguagePreference_TracksActiveLanguages() {
        preferenceService.setUserLanguagePreference(meeting.getId(), participant.getId(), "en");
        preferenceService.setUserLanguagePreference(meeting.getId(), host.getId(), "hi");

        var activeLanguages = preferenceService.getActiveTargetLanguages(meeting.getId());
        assertThat(activeLanguages).containsExactlyInAnyOrder("en", "hi");

        String userPreference = preferenceService.getUserLanguagePreference(meeting.getId(), participant.getId());
        assertThat(userPreference).isEqualTo("en");
    }

    @Test
    @Order(11)
    void testMeetingIsolation_UnauthorizedAccessDenied() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(outsiderToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/translations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(12)
    void testRestEndpoint_ListTranslations_ReturnsForParticipant() throws TranslationException {
        translationService.translateAndBroadcast(
                meeting.getId(), null, "Hello", "en", "hi", "Host", System.currentTimeMillis()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);

        ResponseEntity<List> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/translations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @Order(13)
    void testRestEndpoint_SetLanguagePreference() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(participantToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("targetLanguage", "mr");

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/meetings/" + meeting.getId() + "/translations/preference",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("targetLanguage", "mr");
        assertThat(response.getBody()).containsEntry("status", "UPDATED");
    }

    @Test
    @Order(14)
    void testLocalProvider_SupportsExpectedLanguagePairs() {
        LocalTranslationProvider localProvider = new LocalTranslationProvider();
        assertThat(localProvider.supportsLanguagePair("en", "hi")).isTrue();
        assertThat(localProvider.supportsLanguagePair("hi", "en")).isTrue();
        assertThat(localProvider.supportsLanguagePair("en", "mr")).isTrue();
        assertThat(localProvider.supportsLanguagePair("mr", "en")).isTrue();
        assertThat(localProvider.supportsLanguagePair("hi", "mr")).isTrue();
        assertThat(localProvider.supportsLanguagePair("fr", "en")).isFalse();
        assertThat(localProvider.supportsLanguagePair("en", null)).isFalse();
        assertThat(localProvider.supportsLanguagePair(null, "hi")).isFalse();
    }

    @Test
    @Order(15)
    void testLocalProvider_SameLanguagePassThrough() throws TranslationException {
        LocalTranslationProvider localProvider = new LocalTranslationProvider();
        TranslationResult result = localProvider.translate("Hello", "en", "en");
        assertThat(result.getTranslatedText()).isEqualTo("Hello");
    }

    @Test
    @Order(16)
    void testLocalProvider_TranslatesKnownMeetingPhrases() throws TranslationException {
        LocalTranslationProvider localProvider = new LocalTranslationProvider();

        // en -> hi
        TranslationResult enToHi = localProvider.translate("hello", "en", "hi");
        assertThat(enToHi.getTranslatedText()).contains("नमस्ते");

        // en -> mr
        TranslationResult enToMr = localProvider.translate("hello", "en", "mr");
        assertThat(enToMr.getTranslatedText()).contains("नमस्कार");

        // hi -> en: नमस्ते maps to the English greeting "hi" (both "hello" and "hi" map to नमस्ते en->hi,
        // so the reverse lookup for नमस्ते gives "hi" — the last registered entry)
        TranslationResult hiToEn = localProvider.translate("नमस्ते", "hi", "en");
        assertThat(hiToEn.getTranslatedText()).isNotBlank();
        // Must be an ASCII/English response (not the original Devanagari)
        assertThat(hiToEn.getTranslatedText()).matches(".*[a-zA-Z].*");
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private User createUser(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPassword("hashedPassword");
        return userRepository.save(u);
    }
}

/*
 * ============================================================
 * MANUAL TEST SCENARIO — User A (Hindi) → User B (English)
 * ============================================================
 *
 * Prerequisites:
 *   1. Spring Boot server running locally.
 *   2. Both users authenticated and joined the same meeting.
 *
 * Step 1: User B sets language preference
 *   POST /api/meetings/{meetingId}/translations/preference
 *   Authorization: Bearer <user_b_token>
 *   Body: { "targetLanguage": "en" }
 *   Expected: 200 OK, status: UPDATED
 *
 * Step 2: User A speaks in Hindi (client sends transcript)
 *   POST /api/meetings/{meetingId}/translations/live
 *   Authorization: Bearer <user_a_token>
 *   Body: {
 *     "sourceText": "नमस्ते, मीटिंग शुरू करते हैं",
 *     "sourceLanguage": "hi",
 *     "targetLanguage": "en",
 *     "speaker": "User A"
 *   }
 *
 * Step 3: User B subscribes via WebSocket/STOMP:
 *   SUBSCRIBE /topic/meetings/{meetingId}/translations/en
 *   Expected frame payload (LiveTranslationDto):
 *   {
 *     "sourceLanguage": "hi",
 *     "targetLanguage": "en",
 *     "sourceText": "नमस्ते, मीटिंग शुरू करते हैं",
 *     "translatedText": "Hello, let's start the meeting",
 *     "speaker": "User A"
 *   }
 *
 * Latency Observations (LocalTranslationProvider):
 *   - Dictionary lookup is O(1) hash map — sub-millisecond latency.
 *   - No external calls, zero network overhead.
 *   - Full phrase translations require exact match; partial word translation
 *     happens via token-by-token fallback.
 *
 * Accuracy Limitations:
 *   - Only pre-registered phrases/words are translated.
 *   - Sentence structure (grammar) is NOT rearranged.
 *   - Phrases not in dictionary are passed through as-is (untranslated tokens).
 *   - For production: integrate LibreTranslate or Azure Translator for full NMT.
 */
