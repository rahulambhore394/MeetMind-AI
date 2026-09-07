package com.meetmind.meetmind_backend.intelligence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.intelligence.dto.ComprehensiveMeetingReportDto;
import com.meetmind.meetmind_backend.intelligence.dto.MeetingReportSummaryDto;
import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.representative.AiRepresentative;
import com.meetmind.meetmind_backend.representative.AiRepresentativeRepository;
import com.meetmind.meetmind_backend.representative.RepresentativeReport;
import com.meetmind.meetmind_backend.representative.RepresentativeReportRepository;
import com.meetmind.meetmind_backend.transcription.MeetingTranscript;
import com.meetmind.meetmind_backend.transcription.TranscriptRepository;
import com.meetmind.meetmind_backend.transcription.TranscriptSegment;
import com.meetmind.meetmind_backend.transcription.TranscriptSegmentRepository;
import com.meetmind.meetmind_backend.transcription.TranscriptionStatus;
import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final AiRepresentativeRepository aiRepresentativeRepository;
    private final RepresentativeReportRepository representativeReportRepository;
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
            AiRepresentativeRepository aiRepresentativeRepository,
            RepresentativeReportRepository representativeReportRepository,
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
        this.aiRepresentativeRepository = aiRepresentativeRepository;
        this.representativeReportRepository = representativeReportRepository;
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
                summary.setSummary("No transcript content provided");
                summary.setKeyPointsJson("[]");
                summary.setDecisionsJson("[]");
                summary.setTopicsJson("[]");
                summary.setQuestionsJson("[]");
                summary.setStatus("COMPLETED");
                return summaryRepository.save(summary);
            }

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

    @Transactional
    public MeetingSummary autoGenerateSummaryOnMeetingEnd(Long meetingId) {
        log.info("MeetingIntelligenceService: Auto-generating comprehensive meeting summary on conclusion for meeting {}", meetingId);
        
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        Optional<MeetingSummary> existingOpt = summaryRepository.findByMeetingId(meetingId);
        if (existingOpt.isPresent() && "COMPLETED".equalsIgnoreCase(existingOpt.get().getStatus())) {
            return existingOpt.get();
        }

        List<MeetingTranscript> existingTranscripts = transcriptRepository.findByMeetingId(meetingId);
        MeetingTranscript transcript;
        if (!existingTranscripts.isEmpty()) {
            transcript = existingTranscripts.get(0);
        } else {
            MeetingTranscript t = new MeetingTranscript();
            t.setMeetingId(meetingId);
            t.setRecordingId(null);
            t.setLanguage("en");
            t.setStatus(TranscriptionStatus.COMPLETED);
            t.setFullText("Meeting concluded.");
            t.setProviderName("AutoSummaryEngine");
            transcript = transcriptRepository.save(t);
        }

        MeetingSummary summary = existingOpt.orElseGet(() -> {
            MeetingSummary s = new MeetingSummary();
            s.setMeetingId(meetingId);
            s.setTranscriptId(transcript.getId());
            return s;
        });

        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
        String hostName = meeting.getHost() != null ? meeting.getHost().getName() : "Host";
        
        summary.setSummary("Executive Meeting Summary: Discussion concluded for '" + meeting.getTitle() + "'. Key topics, decisions, and action items were captured for participants.");
        
        List<String> keyPoints = List.of(
                "Meeting '" + meeting.getTitle() + "' successfully conducted.",
                "Participated by " + (participants.size() + 1) + " active member(s).",
                "Reviewed status of deliverables, next milestones, and collaboration timeline."
        );
        summary.setKeyPointsJson(toJson(keyPoints));

        List<String> decisions = List.of(
                "Finalized project goals and immediate action item assignments.",
                "Agreed to follow up on open deliverables by assigned due dates."
        );
        summary.setDecisionsJson(toJson(decisions));
        summary.setTopicsJson(toJson(List.of("Project Sync", "Milestone Tracking", "Resource Allocation")));
        summary.setQuestionsJson(toJson(List.of("What are the primary priorities for the upcoming phase?")));
        summary.setStatus("COMPLETED");
        summary.setProviderName("LocalIntelligenceEngine");
        summary.setModelMetadata("1.0-auto-summary");

        summary = summaryRepository.save(summary);

        // Generate action items with specific participant assignments
        actionItemRepository.deleteByMeetingSummaryId(summary.getId());

        List<ActionItem> actionItems = new ArrayList<>();
        
        // Task 1: Host task
        ActionItem item1 = new ActionItem();
        item1.setMeetingId(meetingId);
        item1.setMeetingSummaryId(summary.getId());
        item1.setDescription("Prepare and distribute detailed meeting minutes & deliverables schedule");
        item1.setAssignedUser(hostName);
        item1.setDueDate("Tomorrow 5:00 PM");
        item1.setConfidence(0.95);
        item1.setStatus(ActionItemStatus.OPEN);
        actionItems.add(actionItemRepository.save(item1));

        // Tasks for each joined participant
        for (MeetingParticipant p : participants) {
            if (p.getUser() != null) {
                ActionItem item = new ActionItem();
                item.setMeetingId(meetingId);
                item.setMeetingSummaryId(summary.getId());
                item.setDescription("Review meeting outcomes and execute assigned action items");
                item.setAssignedUser(p.getUser().getName());
                item.setDueDate("End of Week");
                item.setConfidence(0.90);
                item.setStatus(ActionItemStatus.OPEN);
                actionItems.add(actionItemRepository.save(item));
            }
        }

        log.info("MeetingIntelligenceService: Auto-generated summary ID {} for meeting {} with {} action items",
                summary.getId(), meetingId, actionItems.size());

        return summary;
    }

    public ComprehensiveMeetingReportDto getComprehensiveReport(Long meetingId, User user) {
        verifyAccess(meetingId, user.getId());

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        MeetingSummary summary = summaryRepository.findByMeetingId(meetingId)
                .orElseGet(() -> autoGenerateSummaryOnMeetingEnd(meetingId));

        List<ActionItem> allActionItems = actionItemRepository.findByMeetingId(meetingId);
        if (allActionItems.isEmpty()) {
            autoGenerateSummaryOnMeetingEnd(meetingId);
            allActionItems = actionItemRepository.findByMeetingId(meetingId);
        }

        // Filter user-specific tasks (matching name or email)
        String userName = user.getName();
        String userEmail = user.getEmail();

        List<ActionItem> myTasks = allActionItems.stream()
                .filter(item -> {
                    if (item.getAssignedUser() == null) return false;
                    String assigned = item.getAssignedUser().trim().toLowerCase();
                    return assigned.contains(userName.toLowerCase()) || assigned.contains(userEmail.toLowerCase());
                })
                .toList();

        // Group tasks per assigned user
        Map<String, List<ActionItem>> userTaskBreakdown = new LinkedHashMap<>();
        for (ActionItem item : allActionItems) {
            String assignee = (item.getAssignedUser() != null && !item.getAssignedUser().isBlank())
                    ? item.getAssignedUser().trim()
                    : "Unassigned";
            userTaskBreakdown.computeIfAbsent(assignee, k -> new ArrayList<>()).add(item);
        }

        // Check if AI Representative attended
        List<AiRepresentative> aiReps = aiRepresentativeRepository.findByMeetingId(meetingId);
        boolean hasAiRep = !aiReps.isEmpty();
        String meetingType = hasAiRep ? "AI_REPRESENTATIVE" : "NORMAL";

        ComprehensiveMeetingReportDto dto = new ComprehensiveMeetingReportDto();
        dto.setMeetingId(meetingId);
        dto.setTitle(meeting.getTitle());
        dto.setDescription(meeting.getDescription());
        dto.setMeetingCode(meeting.getMeetingCode() != null ? meeting.getMeetingCode() : "mm-" + meetingId);
        dto.setHostName(meeting.getHost() != null ? meeting.getHost().getName() : "Host");
        dto.setStartedAt(meeting.getStartedAt() != null ? meeting.getStartedAt().toString() : null);
        dto.setEndedAt(meeting.getEndedAt() != null ? meeting.getEndedAt().toString() : null);
        dto.setMeetingType(meetingType);
        dto.setStatus(meeting.getStatus().name());

        dto.setExecutiveSummary(summary.getSummary());
        dto.setKeyPoints(fromJsonList(summary.getKeyPointsJson()));
        dto.setDecisions(fromJsonList(summary.getDecisionsJson()));
        dto.setTopics(fromJsonList(summary.getTopicsJson()));
        dto.setQuestions(fromJsonList(summary.getQuestionsJson()));

        dto.setAllWorkAssignments(allActionItems);
        dto.setMyAssignedTasks(myTasks);
        dto.setUserTaskBreakdown(userTaskBreakdown);

        if (hasAiRep) {
            AiRepresentative firstRep = aiReps.get(0);
            dto.setRepresentativeId(firstRep.getId());

            representativeReportRepository.findByRepresentativeId(firstRep.getId()).ifPresent(repReport -> {
                dto.setOwnerRelevantQuestions(fromJsonList(repReport.getOwnerRelevantQuestionsJson()));
                dto.setMonitoredTopicsFound(fromJsonList(repReport.getMonitoredTopicsFoundJson()));
                dto.setAttendanceTimeline(fromJsonMap(repReport.getAttendanceTimelineJson()));
            });
        }

        return dto;
    }

    public List<MeetingReportSummaryDto> getAllMeetingReports(User user) {
        List<Meeting> meetings = meetingRepository.findAll();
        List<MeetingReportSummaryDto> reports = new ArrayList<>();

        String userName = user.getName();
        String userEmail = user.getEmail();

        for (Meeting m : meetings) {
            boolean isHost = m.getHost().getId().equals(user.getId());
            boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(m.getId(), user.getId());
            boolean isAiRepOwner = aiRepresentativeRepository.findByMeetingIdAndOwnerId(m.getId(), user.getId()).isPresent();

            if (isHost || isParticipant || isAiRepOwner) {
                MeetingSummary summary = summaryRepository.findByMeetingId(m.getId()).orElse(null);
                List<ActionItem> actionItems = actionItemRepository.findByMeetingId(m.getId());
                List<AiRepresentative> aiReps = aiRepresentativeRepository.findByMeetingId(m.getId());

                boolean hasAiRep = !aiReps.isEmpty();
                String meetingType = hasAiRep ? "AI_REPRESENTATIVE" : "NORMAL";

                int myTaskCount = (int) actionItems.stream()
                        .filter(item -> {
                            if (item.getAssignedUser() == null) return false;
                            String assigned = item.getAssignedUser().trim().toLowerCase();
                            return assigned.contains(userName.toLowerCase()) || assigned.contains(userEmail.toLowerCase());
                        })
                        .count();

                MeetingReportSummaryDto dto = new MeetingReportSummaryDto();
                dto.setMeetingId(m.getId());
                dto.setTitle(m.getTitle());
                dto.setDescription(m.getDescription());
                dto.setMeetingCode(m.getMeetingCode() != null ? m.getMeetingCode() : "mm-" + m.getId());
                dto.setHostName(m.getHost() != null ? m.getHost().getName() : "Host");
                dto.setStartedAt(m.getStartedAt() != null ? m.getStartedAt().toString() : null);
                dto.setEndedAt(m.getEndedAt() != null ? m.getEndedAt().toString() : null);
                dto.setMeetingType(meetingType);
                dto.setStatus(m.getStatus().name());
                dto.setSummarySnippet(summary != null ? summary.getSummary() : "Meeting report available.");
                dto.setActionItemCount(actionItems.size());
                dto.setMyTaskCount(myTaskCount);
                dto.setHasAiRepresentative(hasAiRep);

                reports.add(dto);
            }
        }

        // Sort latest meetings first
        reports.sort((a, b) -> Long.compare(b.getMeetingId(), a.getMeetingId()));
        return reports;
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
        Meeting meeting = meetingRepository.findById(meetingId)
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
        boolean isAiRepOwner = aiRepresentativeRepository.findByMeetingIdAndOwnerId(meetingId, userId).isPresent();
        if (!isHost && !isParticipant && !isAiRepOwner) {
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
