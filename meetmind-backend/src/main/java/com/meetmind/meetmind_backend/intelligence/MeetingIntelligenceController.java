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
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        
        ActionItemStatus status = ActionItemStatus.valueOf(statusStr);
        ActionItem updated = intelligenceService.updateActionItemStatus(meetingId, actionItemId, status, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/api/meetings/{meetingId}/intelligence/generate")
    public ResponseEntity<MeetingIntelligenceService.SummaryDetailView> triggerAnalysis(
            @PathVariable Long meetingId,
            @RequestBody(required = false) Map<String, Object> requestBody,
            Authentication authentication
    ) {
        User user = extractUser(authentication);
        Long transcriptId = null;
        if (requestBody != null && requestBody.containsKey("transcriptId")) {
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
