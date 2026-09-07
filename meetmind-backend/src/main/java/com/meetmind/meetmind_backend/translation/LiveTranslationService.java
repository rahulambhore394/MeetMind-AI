package com.meetmind.meetmind_backend.translation;

import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class LiveTranslationService {

    private static final Logger log = LoggerFactory.getLogger(LiveTranslationService.class);

    private final TranslationProvider provider;
    private final LiveTranslationRepository translationRepository;
    private final TranslationPreferenceService preferenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;

    public LiveTranslationService(
            TranslationProvider provider,
            LiveTranslationRepository translationRepository,
            TranslationPreferenceService preferenceService,
            SimpMessagingTemplate messagingTemplate,
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository
    ) {
        this.provider = provider;
        this.translationRepository = translationRepository;
        this.preferenceService = preferenceService;
        this.messagingTemplate = messagingTemplate;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
    }

    /**
     * Translates a single text segment into a specific target language, persists it,
     * and broadcasts it to the corresponding language-specific STOMP topic.
     */
    @Transactional
    public LiveTranslation translateAndBroadcast(
            Long meetingId,
            Long segmentId,
            String sourceText,
            String sourceLanguage,
            String targetLanguage,
            String speaker,
            Long timestamp
    ) throws TranslationException {

        if (meetingId == null) {
            throw new TranslationException("Meeting ID cannot be null");
        }
        if (sourceText == null || sourceText.isBlank()) {
            throw new TranslationException("Source text cannot be null or blank");
        }

        String srcLang = sourceLanguage != null ? sourceLanguage.trim().toLowerCase() : "en";
        String tgtLang = targetLanguage != null ? targetLanguage.trim().toLowerCase() : "en";

        // Idempotency: Skip if already translated for this segment & target language
        if (segmentId != null && translationRepository.existsByTranscriptSegmentIdAndTargetLanguage(segmentId, tgtLang)) {
            log.info("LiveTranslationService: Duplicate translation for segment {} and lang {}. Skipping.",
                    segmentId, tgtLang);
            List<LiveTranslation> existing = translationRepository.findByTranscriptSegmentId(segmentId);
            return existing.stream()
                    .filter(t -> t.getTargetLanguage().equalsIgnoreCase(tgtLang))
                    .findFirst()
                    .orElse(null);
        }

        TranslationResult result;
        if (srcLang.equalsIgnoreCase(tgtLang)) {
            result = new TranslationResult(sourceText, sourceText, srcLang, tgtLang, provider.providerName());
        } else {
            result = provider.translate(sourceText, srcLang, tgtLang);
        }

        LiveTranslation entity = new LiveTranslation();
        entity.setMeetingId(meetingId);
        entity.setTranscriptSegmentId(segmentId);
        entity.setSourceLanguage(srcLang);
        entity.setTargetLanguage(tgtLang);
        entity.setSourceText(sourceText);
        String safeTranslatedText = (result != null && result.getTranslatedText() != null)
                ? result.getTranslatedText()
                : sourceText;
        entity.setTranslatedText(safeTranslatedText);
        entity.setSpeaker(speaker);
        entity.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
        entity.setProviderName(result != null ? result.getProvider() : provider.providerName());

        LiveTranslation saved = translationRepository.save(entity);

        // Real-time delivery: broadcast ONLY to the target language topic
        String destination = "/topic/meetings/" + meetingId + "/translations/" + tgtLang;
        LiveTranslationDto dto = new LiveTranslationDto(
                saved.getId(),
                saved.getMeetingId(),
                saved.getTranscriptSegmentId(),
                saved.getSourceLanguage(),
                saved.getTargetLanguage(),
                saved.getSourceText(),
                saved.getTranslatedText(),
                saved.getSpeaker(),
                saved.getTimestamp()
        );

        try {
            messagingTemplate.convertAndSend(destination, dto);
            log.debug("LiveTranslationService: Broadcasted translation {} to {}", saved.getId(), destination);
        } catch (Exception e) {
            log.warn("LiveTranslationService: Failed to broadcast to WebSocket {}: {}", destination, e.getMessage());
        }

        return saved;
    }

    /**
     * Translates a live transcript segment to all active requested target languages in the meeting.
     */
    @Transactional
    public List<LiveTranslation> processLiveTranscriptSegment(
            Long meetingId,
            Long segmentId,
            String sourceText,
            String sourceLanguage,
            String speaker,
            Long timestamp
    ) {
        Set<String> activeTargetLanguages = preferenceService.getActiveTargetLanguages(meetingId);
        if (activeTargetLanguages == null || activeTargetLanguages.isEmpty()) {
            // Default to English if no specific preferences are registered
            activeTargetLanguages = Set.of("en");
        }

        List<LiveTranslation> results = new ArrayList<>();
        for (String targetLang : activeTargetLanguages) {
            try {
                LiveTranslation translation = translateAndBroadcast(
                        meetingId,
                        segmentId,
                        sourceText,
                        sourceLanguage,
                        targetLang,
                        speaker,
                        timestamp
                );
                if (translation != null) {
                    results.add(translation);
                }
            } catch (TranslationException e) {
                log.error("LiveTranslationService: Failed translating segment {} to {}: {}",
                        segmentId, targetLang, e.getMessage());
            }
        }
        return results;
    }

    /**
     * Retrieves persisted translations for a meeting.
     */
    public List<LiveTranslation> getTranslations(Long meetingId, String targetLanguage, User user) {
        verifyAccess(meetingId, user.getId());

        if (targetLanguage != null && !targetLanguage.isBlank()) {
            return translationRepository.findByMeetingIdAndTargetLanguageOrderByCreatedAtAsc(
                    meetingId, targetLanguage.trim().toLowerCase());
        }
        return translationRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
    }

    private void verifyAccess(Long meetingId, Long userId) {
        boolean isMeetingPresent = meetingRepository.existsById(meetingId);
        if (!isMeetingPresent) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found");
        }
        boolean isHost = meetingRepository.findById(meetingId)
                .map(m -> m.getHost().getId().equals(userId))
                .orElse(false);
        boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
        if (!isHost && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: user is not part of this meeting");
        }
    }

    /**
     * DTO payload for real-time WebSocket translation messages.
     */
    public record LiveTranslationDto(
            Long id,
            Long meetingId,
            Long transcriptSegmentId,
            String sourceLanguage,
            String targetLanguage,
            String sourceText,
            String translatedText,
            String speaker,
            Long timestamp
    ) {}
}
