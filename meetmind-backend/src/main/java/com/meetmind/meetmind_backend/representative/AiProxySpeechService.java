package com.meetmind.meetmind_backend.representative;

import com.meetmind.meetmind_backend.chat.ChatMessage;
import com.meetmind.meetmind_backend.chat.ChatMessageRepository;
import com.meetmind.meetmind_backend.chat.dto.ChatMessageResponse;
import com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiProxySpeechService {

    private static final Logger log = LoggerFactory.getLogger(AiProxySpeechService.class);

    private final AiRepresentativeService representativeService;
    private final AiRepresentativeRepository representativeRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AiProxySpeechService(
            AiRepresentativeService representativeService,
            AiRepresentativeRepository representativeRepository,
            UserRepository userRepository,
            ChatMessageRepository chatMessageRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.representativeService = representativeService;
        this.representativeRepository = representativeRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Inspects an incoming chat message from a meeting participant and determines if any
     * active AI Representative should respond with spoken synthesized voice.
     */
    @Transactional
    public List<AiProxySpeechMessage> handleIncomingChatMessage(Long meetingId, Long senderId, String senderName, String messageText) {
        if (meetingId == null || messageText == null || messageText.isBlank()) {
            return List.of();
        }

        List<AiRepresentative> activeReps = representativeService.getActiveRepresentativesForMeeting(meetingId);
        if (activeReps.isEmpty()) {
            return List.of();
        }

        List<AiProxySpeechMessage> responses = new ArrayList<>();
        String normalizedMsg = messageText.trim().toLowerCase(Locale.ROOT);

        for (AiRepresentative rep : activeReps) {
            // Avoid responding to message sent by the representative's owner themselves
            if (rep.getOwnerId() != null && rep.getOwnerId().equals(senderId)) {
                continue;
            }

            User owner = userRepository.findById(rep.getOwnerId()).orElse(null);
            String ownerName = owner != null ? owner.getName() : "Owner #" + rep.getOwnerId();
            String ownerFirstName = ownerName.split("\\s+")[0].toLowerCase(Locale.ROOT);

            List<String> monitoredTopics = representativeService.fromJsonList(rep.getMonitoredTopicsJson());
            List<String> monitoredQuestions = representativeService.fromJsonList(rep.getMonitoredQuestionsJson());
            List<com.meetmind.meetmind_backend.representative.dto.PredefinedQa> predefinedQaList =
                    representativeService.parsePredefinedQa(rep.getPredefinedQaJson());

            boolean isDirectMention = normalizedMsg.contains("@ai") ||
                    normalizedMsg.contains("@proxy") ||
                    normalizedMsg.contains("@representative") ||
                    (!ownerFirstName.isBlank() && normalizedMsg.contains("@" + ownerFirstName)) ||
                    (!ownerFirstName.isBlank() && ownerFirstName.length() >= 3 && 
                     java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(ownerFirstName) + "\\b").matcher(normalizedMsg).find());

            // 1. Check Predefined Questions & Answers (High Priority)
            boolean matchesPredefined = false;
            String matchedAnswer = "";
            for (com.meetmind.meetmind_backend.representative.dto.PredefinedQa qa : predefinedQaList) {
                if (qa.question() != null && !qa.question().isBlank()) {
                    String cleanQ = qa.question().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim();
                    if (isQuestionMatch(normalizedMsg, cleanQ)) {
                        matchesPredefined = true;
                        matchedAnswer = (qa.answer() != null) ? qa.answer().trim() : "";
                        break;
                    }
                }
            }

            boolean matchesTopic = false;
            String matchedTopicName = "";
            for (String topic : monitoredTopics) {
                if (!topic.isBlank() && normalizedMsg.contains(topic.toLowerCase(Locale.ROOT).trim())) {
                    matchesTopic = true;
                    matchedTopicName = topic.trim();
                    break;
                }
            }

            boolean matchesQuestion = false;
            String matchedQuestionText = "";
            for (String q : monitoredQuestions) {
                if (!q.isBlank()) {
                    String cleanQ = q.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim();
                    if (isQuestionMatch(normalizedMsg, cleanQ)) {
                        matchesQuestion = true;
                        matchedQuestionText = q.trim();
                        break;
                    }
                }
            }

            if (matchesPredefined || isDirectMention || matchesTopic || matchesQuestion) {
                log.info("AiProxySpeechService: Active representative {} for owner {} triggered in meeting {} (matchesPredefined={}) by message: '{}'",
                        rep.getId(), ownerName, meetingId, matchesPredefined, messageText);

                String spokenAnswer;
                if (matchesPredefined && !matchedAnswer.isBlank()) {
                    spokenAnswer = "Speaking on behalf of " + ownerName + ": " + matchedAnswer;
                } else {
                    spokenAnswer = formulateSpeechResponse(ownerName, messageText, matchedTopicName, matchedQuestionText);
                }

                AiProxySpeechMessage speechMessage = dispatchSpeech(meetingId, rep, owner, messageText, spokenAnswer, "en");
                responses.add(speechMessage);
            }
        }

        return responses;
    }

    /**
     * Directly asks the AI Representative a query and synthesizes a spoken response.
     */
    @Transactional
    public AiProxySpeechMessage directQueryRepresentative(Long meetingId, Long representativeId, String query, String language) {
        AiRepresentative rep = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new IllegalArgumentException("AI Representative not found: " + representativeId));

        if (!rep.getMeetingId().equals(meetingId)) {
            throw new IllegalArgumentException("Representative " + representativeId + " does not belong to meeting " + meetingId);
        }

        User owner = userRepository.findById(rep.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getName() : "Representative";

        List<com.meetmind.meetmind_backend.representative.dto.PredefinedQa> predefinedQaList =
                representativeService.parsePredefinedQa(rep.getPredefinedQaJson());
        String cleanQuery = query.toLowerCase(Locale.ROOT).trim();
        String spokenAnswer = "";
        for (com.meetmind.meetmind_backend.representative.dto.PredefinedQa qa : predefinedQaList) {
            if (qa.question() != null && isQuestionMatch(cleanQuery, qa.question().toLowerCase(Locale.ROOT).trim())) {
                spokenAnswer = "Speaking on behalf of " + ownerName + ": " + (qa.answer() != null ? qa.answer().trim() : "");
                break;
            }
        }
        if (spokenAnswer.isBlank()) {
            spokenAnswer = formulateSpeechResponse(ownerName, query, "", "");
        }
        return dispatchSpeech(meetingId, rep, owner, query, spokenAnswer, language != null ? language : "en");
    }

    private boolean isQuestionMatch(String message, String cleanQuestion) {
        if (message == null || cleanQuestion == null || cleanQuestion.isBlank()) return false;
        if (message.contains(cleanQuestion)) return true;

        String[] qWords = cleanQuestion.replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        int meaningfulWords = 0;
        int matchedWords = 0;
        for (String w : qWords) {
            if (w.length() >= 3) {
                meaningfulWords++;
                if (message.contains(w)) {
                    matchedWords++;
                }
            }
        }
        if (meaningfulWords == 0) return false;
        double ratio = (double) matchedWords / meaningfulWords;
        return (meaningfulWords <= 2 && matchedWords >= 1) || (ratio >= 0.5);
    }

    /**
     * Broadcasts speech event to STOMP WebSocket topic and publishes text fallback into meeting chat.
     */
    private AiProxySpeechMessage dispatchSpeech(
            Long meetingId,
            AiRepresentative rep,
            User owner,
            String query,
            String spokenText,
            String language
    ) {
        String ownerName = owner != null ? owner.getName() : "Owner #" + rep.getOwnerId();
        long timestamp = System.currentTimeMillis();

        AiProxySpeechMessage speechMessage = new AiProxySpeechMessage(
                meetingId,
                rep.getId(),
                rep.getOwnerId(),
                ownerName,
                query,
                spokenText,
                language != null ? language : "en",
                timestamp,
                rep.getAvatarUrl()
        );

        // 1. Broadcast synthesized speech event for in-call voice generation
        String speechDestination = "/topic/meetings/" + meetingId + "/ai-proxy/speech";
        try {
            messagingTemplate.convertAndSend(speechDestination, speechMessage);
            log.info("AiProxySpeechService: Broadcasted speech event for {} to {}", ownerName, speechDestination);
        } catch (Exception e) {
            log.error("AiProxySpeechService: Failed to broadcast speech event to {}: {}", speechDestination, e.getMessage());
        }

        // 2. Persist and broadcast synchronized chat subtitle
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMeetingId(meetingId);
            chatMessage.setSenderId(rep.getOwnerId());
            chatMessage.setMessage("[AI Proxy for " + ownerName + "]: " + spokenText);
            chatMessage.setMessageType("AI_REPRESENTATIVE_SPEECH");
            ChatMessage saved = chatMessageRepository.save(chatMessage);

            ChatMessageResponse chatResponse = new ChatMessageResponse();
            chatResponse.setMessageId(saved.getId());
            chatResponse.setMeetingId(meetingId);
            chatResponse.setSenderId(rep.getOwnerId());
            chatResponse.setSenderName("AI Proxy (" + ownerName + ")");
            chatResponse.setMessage(saved.getMessage());
            chatResponse.setMessageType(saved.getMessageType());
            chatResponse.setSentAt(saved.getCreatedAt());

            messagingTemplate.convertAndSend("/topic/meetings/" + meetingId + "/chat", chatResponse);
        } catch (Exception e) {
            log.warn("AiProxySpeechService: Failed to persist/broadcast chat message fallback: {}", e.getMessage());
        }

        return speechMessage;
    }

    /**
     * Generates a context-aware verbal response formulated in the persona of the owner's AI representative.
     */
    private String formulateSpeechResponse(String ownerName, String query, String matchedTopic, String matchedQuestion) {
        String cleanQuery = query.toLowerCase(Locale.ROOT);

        if (!matchedTopic.isBlank()) {
            return "Hi team, speaking on behalf of " + ownerName + ": Regarding " + matchedTopic +
                    ", this is a high-priority item for our track. We are progressing according to schedule and all deliverables remain aligned.";
        }

        if (!matchedQuestion.isBlank()) {
            return "Speaking for " + ownerName + ": In response to the question regarding " + matchedQuestion +
                    ", our current status is verified and on track. I am documenting this for " + ownerName + "'s follow-up report.";
        }

        if (cleanQuery.contains("where is") || cleanQuery.contains("is " + ownerName.toLowerCase(Locale.ROOT) + " here") || cleanQuery.contains("available")) {
            return "Hello everyone. I am " + ownerName + "'s autonomous AI Representative. " + ownerName +
                    " is currently unable to attend, but I am actively monitoring the discussion, tracking action items, and will submit a complete briefing.";
        }

        if (cleanQuery.contains("update") || cleanQuery.contains("status")) {
            return "On behalf of " + ownerName + ": All ongoing action items and milestone targets are on schedule with no blockers to report.";
        }

        return "Speaking for " + ownerName + ": I have registered your inquiry regarding this topic and will ensure " +
                ownerName + " receives it directly in the post-meeting intelligence summary.";
    }
}
