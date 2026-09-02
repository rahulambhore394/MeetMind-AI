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
        String targetLanguage = body.getOrDefault("targetLanguage", "en");
        preferenceService.setUserLanguagePreference(meetingId, user.getId(), targetLanguage);
        return ResponseEntity.ok(Map.of(
                "meetingId", String.valueOf(meetingId),
                "userId", String.valueOf(user.getId()),
                "targetLanguage", targetLanguage,
                "status", "UPDATED"
        ));
    }

    /**
     * Triggers live translation for a text chunk and broadcasts to the relevant STOMP topic.
     */
    @PostMapping("/live")
    public ResponseEntity<LiveTranslation> translateLive(
            @PathVariable Long meetingId,
            @RequestBody LiveTranslateRequest request,
            @AuthenticationPrincipal User user
    ) throws TranslationException {

        String speaker = request.speaker() != null ? request.speaker() : user.getName();
        LiveTranslation result = translationService.translateAndBroadcast(
                meetingId,
                request.segmentId(),
                request.sourceText(),
                request.sourceLanguage(),
                request.targetLanguage(),
                speaker,
                request.timestamp()
        );
        return ResponseEntity.ok(result);
    }

    public record LiveTranslateRequest(
            Long segmentId,
            String sourceText,
            String sourceLanguage,
            String targetLanguage,
            String speaker,
            Long timestamp
    ) {}
}
