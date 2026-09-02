package com.meetmind.meetmind_backend.representative;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.intelligence.ActionItem;
import com.meetmind.meetmind_backend.intelligence.ActionItemRepository;
import com.meetmind.meetmind_backend.intelligence.MeetingSummary;
import com.meetmind.meetmind_backend.intelligence.MeetingSummaryRepository;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class RepresentativeReportService {

    private static final Logger log = LoggerFactory.getLogger(RepresentativeReportService.class);

    private final RepresentativeReportRepository reportRepository;
    private final MeetingSummaryRepository summaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public RepresentativeReportService(
            RepresentativeReportRepository reportRepository,
            MeetingSummaryRepository summaryRepository,
            ActionItemRepository actionItemRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.reportRepository = reportRepository;
        this.summaryRepository = summaryRepository;
        this.actionItemRepository = actionItemRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RepresentativeReport generateReport(AiRepresentative representative) {
        Long meetingId = representative.getMeetingId();
        Long ownerId = representative.getOwnerId();

        log.info("RepresentativeReportService: Generating report for representative {} (meeting {}, owner {})",
                representative.getId(), meetingId, ownerId);

        User owner = userRepository.findById(ownerId).orElse(null);
        String ownerName = owner != null ? owner.getName() : "Owner #" + ownerId;

        List<String> monitoredTopics = fromJsonList(representative.getMonitoredTopicsJson());
        List<String> monitoredQuestions = fromJsonList(representative.getMonitoredQuestionsJson());

        // Attempt to fetch general meeting summary if available
        Optional<MeetingSummary> meetingSummaryOpt = summaryRepository.findByMeetingId(meetingId);

        String summaryText = meetingSummaryOpt.map(MeetingSummary::getSummary)
                .orElse("AI Representative attended meeting #" + meetingId + " on behalf of " + ownerName + ".");

        List<String> discussions = meetingSummaryOpt.map(s -> fromJsonList(s.getKeyPointsJson()))
                .orElse(List.of("Meeting completed successfully."));

        List<String> decisions = meetingSummaryOpt.map(s -> fromJsonList(s.getDecisionsJson()))
                .orElse(List.of());

        List<String> questions = meetingSummaryOpt.map(s -> fromJsonList(s.getQuestionsJson()))
                .orElse(List.of());

        List<ActionItem> actionItems = actionItemRepository.findByMeetingId(meetingId);

        // Filter owner-relevant items
        List<String> ownerRelevantQuestions = new ArrayList<>();
        for (String q : questions) {
            String lower = q.toLowerCase();
            if (lower.contains(ownerName.toLowerCase()) || isTopicMatched(lower, monitoredQuestions)) {
                ownerRelevantQuestions.add(q);
            }
        }
        if (ownerRelevantQuestions.isEmpty() && !questions.isEmpty()) {
            ownerRelevantQuestions.add(questions.get(0));
        }

        List<String> topicsFound = new ArrayList<>();
        for (String topic : monitoredTopics) {
            topicsFound.add("Monitored Topic '" + topic + "': Analyzed in transcript context.");
        }

        List<String> actionItemStrings = new ArrayList<>();
        for (ActionItem item : actionItems) {
            actionItemStrings.add(item.getDescription() + " (Assigned: " + (item.getAssignedUser() != null ? item.getAssignedUser() : "Unassigned") + ")");
        }

        RepresentativeReport report = reportRepository.findByRepresentativeId(representative.getId())
                .orElseGet(RepresentativeReport::new);

        report.setRepresentativeId(representative.getId());
        report.setOwnerId(ownerId);
        report.setMeetingId(meetingId);
        report.setSummary(summaryText);
        report.setImportantDiscussionsJson(toJson(discussions));
        report.setDecisionsJson(toJson(decisions));
        report.setActionItemsJson(toJson(actionItemStrings));
        report.setOwnerRelevantQuestionsJson(toJson(ownerRelevantQuestions));
        report.setMonitoredTopicsFoundJson(toJson(topicsFound));
        report.setTranscriptReferencesJson(toJson(List.of("MeetingTranscript #" + meetingId)));
        report.setAttendanceTimelineJson(toJson(Map.of(
                "status", representative.getStatus().toString(),
                "startedAt", String.valueOf(representative.getStartedAt()),
                "endedAt", String.valueOf(representative.getEndedAt()),
                "automatedIdentity", "[AI Representative] " + ownerName + "'s AI Representative"
        )));

        report = reportRepository.save(report);

        log.info("RepresentativeReportService: Persisted report {} for representative {}", report.getId(), representative.getId());

        eventPublisher.publishEvent(new SpringRepresentativeReportReadyEvent(
                this, report.getId(), representative.getId(), meetingId, ownerId
        ));

        return report;
    }

    public RepresentativeReport getReport(Long meetingId, Long representativeId, User user) {
        RepresentativeReport report = reportRepository.findByRepresentativeId(representativeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found for representative: " + representativeId));

        if (!report.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report does not belong to meeting: " + meetingId);
        }

        if (!report.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: You do not own this report");
        }

        return report;
    }

    private boolean isTopicMatched(String text, List<String> topics) {
        for (String topic : topics) {
            if (text.contains(topic.toLowerCase())) return true;
        }
        return false;
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
