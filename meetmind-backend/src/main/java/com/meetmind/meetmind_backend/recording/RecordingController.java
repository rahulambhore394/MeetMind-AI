package com.meetmind.meetmind_backend.recording;

import com.meetmind.meetmind_backend.user.User;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/{meetingId}/recordings")
public class RecordingController {

    private final RecordingService recordingService;
    private final SimpMessagingTemplate messagingTemplate;

    public RecordingController(RecordingService recordingService, SimpMessagingTemplate messagingTemplate) {
        this.recordingService = recordingService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/start")
    public ResponseEntity<MeetingRecording> startRecording(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user
    ) {
        MeetingRecording recording = recordingService.startRecording(meetingId, user);

        // Broadcast STOMP alert to let participants know recording has started
        messagingTemplate.convertAndSend(
                "/topic/meetings/" + meetingId + "/recordings",
                Map.of(
                        "type", "RECORDING_STARTED",
                        "meetingId", meetingId,
                        "recordingId", recording.getId(),
                        "ownerId", user.getId(),
                        "ownerName", user.getName(),
                        "timestamp", System.currentTimeMillis()
                )
        );

        return ResponseEntity.ok(recording);
    }

    @PostMapping("/{recordingId}/stop")
    public ResponseEntity<MeetingRecording> stopRecording(
            @PathVariable Long meetingId,
            @PathVariable Long recordingId,
            @AuthenticationPrincipal User user
    ) {
        MeetingRecording recording = recordingService.stopRecording(meetingId, recordingId, user);

        // Broadcast STOMP alert to let participants know recording has stopped
        messagingTemplate.convertAndSend(
                "/topic/meetings/" + meetingId + "/recordings",
                Map.of(
                        "type", "RECORDING_STOPPED",
                        "meetingId", meetingId,
                        "recordingId", recordingId,
                        "ownerId", user.getId(),
                        "timestamp", System.currentTimeMillis()
                )
        );

        return ResponseEntity.ok(recording);
    }

    @PostMapping("/{recordingId}/upload")
    public ResponseEntity<MeetingRecording> uploadFile(
            @PathVariable Long meetingId,
            @PathVariable Long recordingId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        MeetingRecording recording = recordingService.uploadRecordingFile(meetingId, recordingId, file, user);
        return ResponseEntity.ok(recording);
    }

    @GetMapping
    public ResponseEntity<List<MeetingRecording>> listRecordings(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(recordingService.listRecordings(meetingId, user));
    }

    @GetMapping("/{recordingId}/download")
    public ResponseEntity<Resource> downloadRecording(
            @PathVariable Long meetingId,
            @PathVariable Long recordingId,
            @AuthenticationPrincipal User user
    ) throws IOException {
        Path filePath = recordingService.getRecordingFile(meetingId, recordingId, user);
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
