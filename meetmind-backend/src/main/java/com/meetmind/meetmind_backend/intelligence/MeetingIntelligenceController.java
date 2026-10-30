package com.meetmind.meetmind_backend.intelligence;

import com.meetmind.meetmind_backend.intelligence.dto.ComprehensiveMeetingReportDto;
import com.meetmind.meetmind_backend.intelligence.dto.MeetingReportSummaryDto;
import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class MeetingIntelligenceController {

    private final MeetingIntelligenceService intelligenceService;

    public MeetingIntelligenceController(MeetingIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/api/meetings/reports/all")
    public ResponseEntity<List<MeetingReportSummaryDto>> getAllMeetingReports(
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<MeetingReportSummaryDto> reports = intelligenceService.getAllMeetingReports(user);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/api/meetings/{meetingId}/comprehensive-report")
    public ResponseEntity<ComprehensiveMeetingReportDto> getComprehensiveReport(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        ComprehensiveMeetingReportDto report = intelligenceService.getComprehensiveReport(meetingId, user);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/api/meetings/{meetingId}/intelligence")
    public ResponseEntity<MeetingIntelligenceService.SummaryDetailView> getIntelligenceSummary(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        MeetingIntelligenceService.SummaryDetailView summary = intelligenceService.getSummaryDetail(meetingId, user);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/api/meetings/{meetingId}/decisions")
    public ResponseEntity<List<String>> listDecisions(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<String> decisions = intelligenceService.listDecisions(meetingId, user);
        return ResponseEntity.ok(decisions);
    }

    @PostMapping("/api/meetings/{meetingId}/decisions")
    public ResponseEntity<List<String>> addDecision(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        String decisionText = body != null ? body.get("decision") : null;
        if (decisionText == null && body != null) {
            decisionText = body.get("text");
        }
        List<String> decisions = intelligenceService.addDecision(meetingId, decisionText, user);
        return ResponseEntity.ok(decisions);
    }

    @DeleteMapping("/api/meetings/{meetingId}/decisions/{decisionIndex}")
    public ResponseEntity<List<String>> deleteDecision(
            @PathVariable Long meetingId,
            @PathVariable int decisionIndex,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<String> decisions = intelligenceService.deleteDecision(meetingId, decisionIndex, user);
        return ResponseEntity.ok(decisions);
    }

    @GetMapping("/api/meetings/{meetingId}/chapters")
    public ResponseEntity<List<TopicChapter>> listChapters(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<TopicChapter> chapters = intelligenceService.listChapters(meetingId, user);
        return ResponseEntity.ok(chapters);
    }

    @GetMapping("/api/meetings/{meetingId}/parking-lot")
    public ResponseEntity<List<ParkingQuestion>> listParkingLot(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<ParkingQuestion> items = intelligenceService.listParkingQuestions(meetingId, user);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/api/meetings/{meetingId}/parking-lot")
    public ResponseEntity<List<ParkingQuestion>> addParkingQuestion(
            @PathVariable Long meetingId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        String question = body != null ? body.get("question") : null;
        String askedBy = body != null ? body.get("askedBy") : null;
        String status = body != null ? body.get("status") : "UNANSWERED";
        List<ParkingQuestion> items = intelligenceService.addParkingQuestion(meetingId, question, askedBy, status, user);
        return ResponseEntity.ok(items);
    }

    @PatchMapping("/api/meetings/{meetingId}/parking-lot/{questionIndex}")
    public ResponseEntity<List<ParkingQuestion>> updateParkingQuestion(
            @PathVariable Long meetingId,
            @PathVariable int questionIndex,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        String status = body != null ? body.get("status") : null;
        String notes = body != null ? body.get("notes") : null;
        List<ParkingQuestion> items = intelligenceService.updateParkingQuestionStatus(meetingId, questionIndex, status, notes, user);
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/api/meetings/{meetingId}/parking-lot/{questionIndex}")
    public ResponseEntity<List<ParkingQuestion>> deleteParkingQuestion(
            @PathVariable Long meetingId,
            @PathVariable int questionIndex,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<ParkingQuestion> items = intelligenceService.deleteParkingQuestion(meetingId, questionIndex, user);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/api/meetings/{meetingId}/action-items")
    public ResponseEntity<List<ActionItem>> listActionItems(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        List<ActionItem> actionItems = intelligenceService.listActionItems(meetingId, user);
        return ResponseEntity.ok(actionItems);
    }

    @PatchMapping("/api/meetings/{meetingId}/action-items/{actionItemId}")
    public ResponseEntity<ActionItem> updateActionItemStatus(
            @PathVariable Long meetingId,
            @PathVariable Long actionItemId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        String statusStr = null;
        if (body != null) {
            if (body.containsKey("status") && body.get("status") != null) {
                statusStr = body.get("status").toString().trim();
            } else if (body.containsKey("completed") && body.get("completed") != null) {
                boolean isDone = Boolean.parseBoolean(body.get("completed").toString());
                statusStr = isDone ? "COMPLETED" : "PENDING";
            } else if (body.containsKey("isCompleted") && body.get("isCompleted") != null) {
                boolean isDone = Boolean.parseBoolean(body.get("isCompleted").toString());
                statusStr = isDone ? "COMPLETED" : "PENDING";
            }
        }

        if (statusStr == null || statusStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status or completed boolean is required");
        }

        ActionItemStatus status;
        try {
            status = ActionItemStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            if ("OPEN".equalsIgnoreCase(statusStr)) {
                status = ActionItemStatus.PENDING;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + statusStr);
            }
        }

        ActionItem updated = intelligenceService.updateActionItemStatus(meetingId, actionItemId, status, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping({"/api/meetings/{meetingId}/intelligence/generate", "/api/meetings/{meetingId}/intelligence/analyze"})
    public ResponseEntity<MeetingIntelligenceService.SummaryDetailView> triggerAnalysis(
            @PathVariable Long meetingId,
            @RequestParam(value = "transcriptId", required = false) Long queryTranscriptId,
            @RequestBody(required = false) Map<String, Object> requestBody,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        Long transcriptId = queryTranscriptId;
        if (transcriptId == null && requestBody != null && requestBody.containsKey("transcriptId")) {
            Object raw = requestBody.get("transcriptId");
            if (raw instanceof Number n) {
                transcriptId = n.longValue();
            } else if (raw != null) {
                try {
                    transcriptId = Long.parseLong(raw.toString());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (transcriptId != null) {
            try {
                MeetingIntelligenceService.SummaryDetailView summary = intelligenceService.triggerAnalysis(meetingId, transcriptId, user);
                return ResponseEntity.ok(summary);
            } catch (Exception e) {
                // fallback to auto-generation
            }
        }

        intelligenceService.autoGenerateSummaryOnMeetingEnd(meetingId);
        return ResponseEntity.ok(intelligenceService.getSummaryDetail(meetingId, user));
    }

    private User extractUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated user");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        if (authentication.getPrincipal() instanceof com.meetmind.meetmind_backend.auth.UserPrincipal up) {
            return up.getUser();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user authentication");
    }
}
