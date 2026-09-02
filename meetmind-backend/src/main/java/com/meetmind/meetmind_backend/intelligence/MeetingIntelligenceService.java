package com.meetmind.meetmind_backend.intelligence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.transcription.MeetingTranscript;
import com.meetmind.meetmind_backend.transcription.TranscriptRepository;
import com.meetmind.meetmind_backend.transcription.TranscriptSegment;
import com.meetmind.meetmind_backend.transcription.TranscriptSegmentRepository;
import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class MeetingIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(MeetingIntelligenceService.class);

    private final MeetingIntelligenceProvider provider;
    private final MeetingSummaryRepository summaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public MeetingIntelligenceService(
            MeetingIntelligenceProvider provider,
            MeetingSummaryRepository summaryRepository,
            ActionItemRepository actionItemRepository,
            TranscriptRepository transcriptRepository,
            TranscriptSegmentRepository segmentRepository,
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.provider = provider;
        this.summaryRepository = summaryRepository;
        this.actionItemRepository = actionItemRepository;
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MeetingSummary generateIntelligence(Long transcriptId) throws IntelligenceException {
        MeetingTranscript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new IllegalArgumentException("Transcript not found: " + transcriptId));

        if (!"COMPLETED".equals(transcript.getStatus().name())) {
            log.warn("MeetingIntelligenceService: Transcript {} is not COMPLETED. Status: {}", transcriptId, transcript.getStatus());
            throw new IntelligenceException("Transcript must be COMPLETED to generate intelligence");
        }

        Long meetingId = transcript.getMeetingId();
        log.info("MeetingIntelligenceService: Generating intelligence for transcript {} (meeting {})", transcriptId, meetingId);

        // Find existing summary or create new with PROCESSING status
        MeetingSummary summary = summaryRepository.findByTranscriptId(transcriptId)
                .orElseGet(() -> {
                    MeetingSummary s = new MeetingSummary();
                    s.setMeetingId(meetingId);
                    s.setTranscriptId(transcriptId);
                    return s;
                });
        
        summary.setStatus("PROCESSING");
        summary = summaryRepository.save(summary);

        try {
            List<TranscriptSegment> segments = segmentRepository.findByTranscriptIdOrderBySegmentIndex(transcriptId);
            String fullText = transcript.getFullText() != null ? transcript.getFullText() : "";

            if (fullText.isBlank() && segments.isEmpty()) {
                summary.setStatus("FAILED");
                summaryRepository.save(summary);
                throw new IntelligenceException("Transcript content is empty");
            }

            // Perform analysis via provider
            IntelligenceResult result = provider.analyze(fullText, segments, transcript.getLanguage());

            summary.setSummary(result.summary());
            summary.setKeyPointsJson(toJson(result.keyPoints()));
            summary.setDecisionsJson(toJson(result.decisions()));
            summary.setTopicsJson(toJson(result.topics()));
            summary.setQuestionsJson(toJson(result.questions()));
            summary.setAnalysisJson(toJson(result.analysisMetrics()));
            summary.setProviderName(result.providerName());
            summary.setModelMetadata(result.modelMetadata());
            summary.setStatus("COMPLETED");

            summary = summaryRepository.save(summary);

            // Delete existing action items for this summary and re-insert
            actionItemRepository.deleteByMeetingSummaryId(summary.getId());

            List<ActionItem> savedItems = new ArrayList<>();
            if (result.actionItems() != null) {
                for (ActionItemData data : result.actionItems()) {
                    ActionItem item = new ActionItem();
                    item.setMeetingId(meetingId);
                    item.setMeetingSummaryId(summary.getId());
                    item.setDescription(data.description());
                    item.setAssignedUser(data.assignedUser());
                    item.setDueDate(data.dueDate());
                    item.setConfidence(data.confidence() != null ? data.confidence() : 0.5);
                    item.setStatus(ActionItemStatus.OPEN);
                    item = actionItemRepository.save(item);
                    savedItems.add(item);

                    if (item.getAssignedUser() != null && !item.getAssignedUser().isBlank()) {
                        eventPublisher.publishEvent(new SpringActionItemAssignedEvent(
                            this, item.getId(), meetingId, item.getAssignedUser(), item.getDescription()
                        ));
                    }
                }
            }

            log.info("MeetingIntelligenceService: Persisted intelligence summary {} for meeting {} with {} action items",
                    summary.getId(), meetingId, savedItems.size());

            // Publish Spring Event for downstream listeners/Kafka
            eventPublisher.publishEvent(new SpringAiSummaryReadyEvent(
                    this,
                    summary.getId(),
                    meetingId,
                    transcriptId,
                    savedItems.size(),
                    true
            ));

            return summary;
        } catch (Exception e) {
            log.error("MeetingIntelligenceService: Intelligence generation failed for transcript {}", transcriptId, e);
            summary.setStatus("FAILED");
            summaryRepository.save(summary);
            throw (e instanceof IntelligenceException) ? (IntelligenceException) e : new IntelligenceException(e.getMessage(), e);
        }
    }

    public SummaryDetailView getSummaryDetail(Long meetingId, User user) {
        verifyAccess(meetingId, user.getId());

        MeetingSummary summary = summaryRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intelligence summary not found for meeting: " + meetingId));

        List<ActionItem> actionItems = actionItemRepository.findByMeetingSummaryId(summary.getId());

        return new SummaryDetailView(
                summary.getId(),
                summary.getMeetingId(),
                summary.getTranscriptId(),
                summary.getSummary(),
                fromJsonList(summary.getKeyPointsJson()),
                fromJsonList(summary.getDecisionsJson()),
                fromJsonList(summary.getTopicsJson()),
                fromJsonList(summary.getQuestionsJson()),
                fromJsonMap(summary.getAnalysisJson()),
                actionItems,
                summary.getStatus(),
                summary.getProviderName(),
                summary.getModelMetadata(),
                summary.getCreatedAt()
        );
    }

    public List<ActionItem> listActionItems(Long meetingId, User user) {
        verifyAccess(meetingId, user.getId());
        return actionItemRepository.findByMeetingId(meetingId);
    }

    @Transactional
    public ActionItem updateActionItemStatus(Long meetingId, Long actionItemId, ActionItemStatus status, User user) {
        verifyAccess(meetingId, user.getId());
        
        ActionItem item = actionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action item not found"));
        
        if (!item.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action item does not belong to this meeting");
        }
        
        item.setStatus(status);
        return actionItemRepository.save(item);
    }

    public SummaryDetailView triggerAnalysis(Long meetingId, Long transcriptId, User user) throws IntelligenceException {
        com.meetmind.meetmind_backend.meeting.Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        if (!meeting.getHost().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the meeting host can trigger AI analysis");
        }

        MeetingTranscript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript not found"));

        if (!transcript.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transcript does not belong to this meeting");
        }

        generateIntelligence(transcriptId);
        return getSummaryDetail(meetingId, user);
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON list", e);
            return List.of();
        }
    }

    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON map", e);
            return Map.of();
        }
    }

    public record SummaryDetailView(
            Long id,
            Long meetingId,
            Long transcriptId,
            String summary,
            List<String> keyPoints,
            List<String> decisions,
            List<String> topics,
            List<String> questions,
            Map<String, Object> analysisMetrics,
            List<ActionItem> actionItems,
            String status,
            String providerName,
            String modelMetadata,
            java.time.LocalDateTime createdAt
    ) {}
}
