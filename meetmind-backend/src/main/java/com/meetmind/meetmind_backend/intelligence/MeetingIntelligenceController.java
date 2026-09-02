package com.meetmind.meetmind_backend.intelligence;

import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/{meetingId}")
public class MeetingIntelligenceController {

    private final MeetingIntelligenceService intelligenceService;

    public MeetingIntelligenceController(MeetingIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/intelligence")
    public ResponseEntity<MeetingIntelligenceService.SummaryDetailView> getIntelligenceSummary(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        MeetingIntelligenceService.SummaryDetailView summary = intelligenceService.getSummaryDetail(meetingId, user);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/action-items")
    public ResponseEntity<List<ActionItem>> listActionItems(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        List<ActionItem> actionItems = intelligenceService.listActionItems(meetingId, user);
        return ResponseEntity.ok(actionItems);
    }

    @PatchMapping("/action-items/{actionItemId}")
    public ResponseEntity<ActionItem> updateActionItemStatus(
            @PathVariable Long meetingId,
            @PathVariable Long actionItemId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        String statusStr = body.get("status");
        if (statusStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        
        ActionItemStatus status = ActionItemStatus.valueOf(statusStr);
        ActionItem updated = intelligenceService.updateActionItemStatus(meetingId, actionItemId, status, user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/intelligence/generate")
    public ResponseEntity<MeetingIntelligenceService.SummaryDetailView> triggerAnalysis(
            @PathVariable Long meetingId,
            @RequestBody(required = false) Map<String, Long> requestBody,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        Long transcriptId = (requestBody != null) ? requestBody.get("transcriptId") : null;

        if (transcriptId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transcriptId is required in request body");
        }

        try {
            MeetingIntelligenceService.SummaryDetailView summary = intelligenceService.triggerAnalysis(meetingId, transcriptId, user);
            return ResponseEntity.ok(summary);
        } catch (IntelligenceException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI Intelligence analysis failed: " + e.getMessage());
        }
    }

    private void verifyAuthenticated(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated request");
        }
    }
}
