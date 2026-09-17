package com.meetmind.meetmind_backend.representative;

import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/{meetingId}/representatives")
public class AiRepresentativeController {

    private final AiRepresentativeService representativeService;
    private final RepresentativeReportService reportService;
    private final AiProxySpeechService speechService;

    public AiRepresentativeController(
            AiRepresentativeService representativeService,
            RepresentativeReportService reportService,
            AiProxySpeechService speechService
    ) {
        this.representativeService = representativeService;
        this.reportService = reportService;
        this.speechService = speechService;
    }

    @PostMapping
    public ResponseEntity<AiRepresentative> createRepresentative(
            @PathVariable Long meetingId,
            @RequestBody CreateRepresentativeRequest request,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        AiRepresentative rep = representativeService.createRepresentative(
                meetingId,
                user,
                request.monitoredTopics() != null ? request.monitoredTopics() : List.of(),
                request.monitoredQuestions() != null ? request.monitoredQuestions() : List.of(),
                request.importantPeople() != null ? request.importantPeople() : List.of(),
                request.reportPreferences(),
                request.notificationPreferences()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(rep);
    }

    @GetMapping
    public ResponseEntity<AiRepresentative> getRepresentative(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        AiRepresentative rep = representativeService.getRepresentativeForMeeting(meetingId, user);
        return ResponseEntity.ok(rep);
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<AiRepresentative> uploadMedia(
            @PathVariable Long meetingId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        String mediaStoragePath = body.get("mediaStoragePath");
        AiRepresentative rep = representativeService.uploadMedia(meetingId, id, mediaStoragePath, user);
        return ResponseEntity.ok(rep);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AiRepresentative> cancelRepresentative(
            @PathVariable Long meetingId,
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        AiRepresentative rep = representativeService.cancelRepresentative(meetingId, id, user);
        return ResponseEntity.ok(rep);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<RepresentativeReport> getReport(
            @PathVariable Long meetingId,
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        RepresentativeReport report = reportService.getReport(meetingId, id, user);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{id}/speak")
    public ResponseEntity<com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage> triggerSpeech(
            @PathVariable Long meetingId,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal User user
    ) {
        verifyAuthenticated(user);
        String query = (body != null && body.containsKey("query")) ? body.get("query") : "What is the status?";
        String language = (body != null && body.containsKey("language")) ? body.get("language") : "en";
        com.meetmind.meetmind_backend.representative.dto.AiProxySpeechMessage response =
                speechService.directQueryRepresentative(meetingId, id, query, language);
        return ResponseEntity.ok(response);
    }

    private void verifyAuthenticated(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated request");
        }
    }

    public record CreateRepresentativeRequest(
            List<String> monitoredTopics,
            List<String> monitoredQuestions,
            List<String> importantPeople,
            String reportPreferences,
            String notificationPreferences
    ) {}
}
