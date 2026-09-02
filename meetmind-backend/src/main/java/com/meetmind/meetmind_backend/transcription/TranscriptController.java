package com.meetmind.meetmind_backend.transcription;

import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/{meetingId}/transcripts")
public class TranscriptController {

    private final TranscriptionService transcriptionService;

    public TranscriptController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    /**
     * List all transcripts for a meeting.
     * Accessible by host and participants.
     */
    @GetMapping
    public ResponseEntity<List<MeetingTranscript>> listTranscripts(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(transcriptionService.listTranscripts(meetingId, user));
    }

    /**
     * Get full transcript detail including ordered segments.
     */
    @GetMapping("/{transcriptId}")
    public ResponseEntity<TranscriptionService.TranscriptDetailView> getTranscript(
            @PathVariable Long meetingId,
            @PathVariable Long transcriptId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(transcriptionService.getTranscript(meetingId, transcriptId, user));
    }

    /**
     * Manually trigger transcription for a recording.
     * Host only — useful for reprocessing or when automatic Kafka trigger was missed.
     *
     * Request body (JSON): { "recordingId": 123, "language": "en" }
     */
    @PostMapping("/trigger")
    public ResponseEntity<MeetingTranscript> triggerTranscription(
            @PathVariable Long meetingId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user
    ) {
        Long recordingId = toLong(body.get("recordingId"));
        String language = (String) body.getOrDefault("language", "en");

        if (recordingId == null) {
            return ResponseEntity.badRequest().build();
        }

        MeetingTranscript transcript = transcriptionService.triggerTranscription(meetingId, recordingId, language, user);
        return ResponseEntity.ok(transcript);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }
}
