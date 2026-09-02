package com.meetmind.meetmind_backend.translation;

import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class TranslationWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(TranslationWebSocketController.class);

    private final LiveTranslationService translationService;
    private final TranslationPreferenceService preferenceService;

    public TranslationWebSocketController(
            LiveTranslationService translationService,
            TranslationPreferenceService preferenceService
    ) {
        this.translationService = translationService;
        this.preferenceService = preferenceService;
    }

    /**
     * Endpoint for live speech / transcript translation requests over WebSocket.
     * Destination: /app/meetings/{meetingId}/translate
     */
    @MessageMapping("/meetings/{meetingId}/translate")
    public void handleLiveTranslate(
            @DestinationVariable Long meetingId,
            @Payload LiveTranslateMessage message,
            Principal principal
    ) {
        String speaker = message.speaker();
        if (speaker == null && principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof User user) {
            speaker = user.getName();
        }

        try {
            if (message.targetLanguage() != null && !message.targetLanguage().isBlank()) {
                translationService.translateAndBroadcast(
                        meetingId,
                        message.segmentId(),
                        message.sourceText(),
                        message.sourceLanguage(),
                        message.targetLanguage(),
                        speaker,
                        message.timestamp()
                );
            } else {
                translationService.processLiveTranscriptSegment(
                        meetingId,
                        message.segmentId(),
                        message.sourceText(),
                        message.sourceLanguage(),
                        speaker,
                        message.timestamp()
                );
            }
        } catch (Exception e) {
            log.error("TranslationWebSocketController: Error translating message in meeting {}: {}",
                    meetingId, e.getMessage());
        }
    }

    /**
     * Endpoint for a user to register or change their preferred translation target language.
     * Destination: /app/meetings/{meetingId}/language-preference
     */
    @MessageMapping("/meetings/{meetingId}/language-preference")
    public void handleLanguagePreference(
            @DestinationVariable Long meetingId,
            @Payload LanguagePreferenceMessage message,
            Principal principal
    ) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof User user) {
            preferenceService.setUserLanguagePreference(meetingId, user.getId(), message.targetLanguage());
            log.info("TranslationWebSocketController: Set language preference '{}' for user {} in meeting {}",
                    message.targetLanguage(), user.getId(), meetingId);
        }
    }

    public record LiveTranslateMessage(
            Long segmentId,
            String sourceText,
            String sourceLanguage,
            String targetLanguage,
            String speaker,
            Long timestamp
    ) {}

    public record LanguagePreferenceMessage(
            String targetLanguage
    ) {}
}
