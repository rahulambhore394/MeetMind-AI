package com.meetmind.meetmind_backend.translation;

import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/{meetingId}/translations")
public class TranslationController {

    private final LiveTranslationService translationService;
    private final TranslationPreferenceService preferenceService;

    public TranslationController(
            LiveTranslationService translationService,
            TranslationPreferenceService preferenceService
    ) {
        this.translationService = translationService;
        this.preferenceService = preferenceService;
    }

    /**
     * Lists translations for a meeting, optionally filtered by targetLanguage.
     * Accessible by participants and host.
     */
    @GetMapping
    public ResponseEntity<List<LiveTranslation>> listTranslations(
            @PathVariable Long meetingId,
            @RequestParam(required = false) String targetLanguage,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<LiveTranslation> translations = translationService.getTranslations(meetingId, targetLanguage, user);
        return ResponseEntity.ok(translations);
    }

    /**
     * Sets user's preferred target language for this meeting.
     */
    @PostMapping("/preference")
    public ResponseEntity<Map<String, String>> setLanguagePreference(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user
    ) {
        Long userId = user != null ? user.getId() : 0L;
        String targetLanguage = body.getOrDefault("targetLanguage", "en");
        preferenceService.setUserLanguagePreference(meetingId, userId, targetLanguage);
        return ResponseEntity.ok(Map.of(
                "meetingId", String.valueOf(meetingId),
                "userId", String.valueOf(userId),
                "targetLanguage", targetLanguage,
                "status", "UPDATED"
        ));
    }

    /**
     * Triggers live translation for a text chunk and broadcasts to the relevant STOMP topic.
     */
    @PostMapping("/live")
    public ResponseEntity<List<LiveTranslation>> translateLive(
            @PathVariable Long meetingId,
            @RequestBody LiveTranslateRequest request,
            @AuthenticationPrincipal User user
    ) throws TranslationException {

        String speaker = request.getSpeaker() != null ? request.getSpeaker() : (user != null ? user.getName() : "Speaker");
        if (request.getTargetLanguage() != null && !request.getTargetLanguage().isBlank()) {
            LiveTranslation result = translationService.translateAndBroadcast(
                    meetingId,
                    request.getSegmentId(),
                    request.getSourceText(),
                    request.getSourceLanguage(),
                    request.getTargetLanguage(),
                    speaker,
                    request.getTimestamp()
            );
            return ResponseEntity.ok(result != null ? List.of(result) : List.of());
        } else {
            List<LiveTranslation> results = translationService.processLiveTranscriptSegment(
                    meetingId,
                    request.getSegmentId(),
                    request.getSourceText(),
                    request.getSourceLanguage(),
                    speaker,
                    request.getTimestamp()
            );
            return ResponseEntity.ok(results);
        }
    }

    public static class LiveTranslateRequest {
        private Long segmentId;
        private String sourceText;
        private String sourceLanguage;
        private String targetLanguage;
        private String speaker;
        private Long timestamp;

        public LiveTranslateRequest() {}

        public Long getSegmentId() { return segmentId; }
        public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

        public String getSourceText() { return sourceText; }
        public void setSourceText(String sourceText) { this.sourceText = sourceText; }

        public String getSourceLanguage() { return sourceLanguage; }
        public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

        public String getTargetLanguage() { return targetLanguage; }
        public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

        public String getSpeaker() { return speaker; }
        public void setSpeaker(String speaker) { this.speaker = speaker; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
