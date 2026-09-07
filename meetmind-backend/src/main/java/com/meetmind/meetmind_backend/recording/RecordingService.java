package com.meetmind.meetmind_backend.recording;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecordingService {
    private static final Logger log = LoggerFactory.getLogger(RecordingService.class);

    private final RecordingRepository recordingRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${recording.storage-dir:uploads/recordings}")
    private String storageDir;

    public RecordingService(
            RecordingRepository recordingRepository,
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.recordingRepository = recordingRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.eventPublisher = eventPublisher;
    }

    public MeetingRecording startRecording(Long meetingId, User user) {
        Meeting meeting = getMeeting(meetingId);
        verifyParticipantOrHost(meeting, user.getId());

        if (meeting.getStatus() != MeetingStatus.LIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting must be Live to record");
        }

        // Prevent repeated starts
        recordingRepository.findByMeetingIdAndStatus(meetingId, RecordingStatus.STARTED)
                .ifPresent(r -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Recording already in progress for this meeting");
                });

        MeetingRecording recording = new MeetingRecording();
        recording.setMeetingId(meetingId);
        recording.setOwnerId(user.getId());
        recording.setStartedAt(LocalDateTime.now());
        recording.setStatus(RecordingStatus.STARTED);

        return recordingRepository.save(recording);
    }

    public MeetingRecording stopRecording(Long meetingId, Long recordingId, User user) {
        Meeting meeting = getMeeting(meetingId);
        verifyParticipantOrHost(meeting, user.getId());

        MeetingRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found"));

        if (!recording.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording does not belong to this meeting");
        }

        if (recording.getStatus() != RecordingStatus.STARTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording is not active");
        }

        recording.setEndedAt(LocalDateTime.now());
        recording.setDuration(Duration.between(recording.getStartedAt(), recording.getEndedAt()).toSeconds());
        recording.setStatus(RecordingStatus.PROCESSING);

        return recordingRepository.save(recording);
    }

    public MeetingRecording uploadRecordingFile(Long meetingId, Long recordingId, MultipartFile file, User user) throws IOException {
        Meeting meeting = getMeeting(meetingId);
        verifyParticipantOrHost(meeting, user.getId());

        MeetingRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found"));

        if (!recording.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording does not belong to this meeting");
        }

        if (recording.getStatus() != RecordingStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording status must be PROCESSING to upload file");
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot upload empty file");
        }

        // Ensure directories exist
        Path uploadPath = Path.of(storageDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Determine filename
        String extension = getFileExtension(file.getOriginalFilename());
        String filename = recordingId + "." + extension;
        Path targetLocation = uploadPath.resolve(filename);

        // Copy file securely
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        recording.setStoragePath(targetLocation.toString());
        recording.setStatus(RecordingStatus.COMPLETED);

        MeetingRecording saved = recordingRepository.save(recording);

        // Publish event to trigger the processing pipeline via Kafka
        String language = resolveLanguageFromMeeting(meeting);
        eventPublisher.publishEvent(new SpringRecordingCompletedEvent(
                this,
                saved.getId(),
                meetingId,
                user.getId(),
                saved.getStoragePath(),
                language
        ));

        return saved;
    }

    public List<MeetingRecording> listRecordings(Long meetingId, User user) {
        Meeting meeting = getMeeting(meetingId);
        verifyParticipantOrHost(meeting, user.getId());

        return recordingRepository.findByMeetingId(meetingId);
    }

    public Path getRecordingFile(Long meetingId, Long recordingId, User user) {
        Meeting meeting = getMeeting(meetingId);
        verifyParticipantOrHost(meeting, user.getId());

        MeetingRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found"));

        if (!recording.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording does not belong to this meeting");
        }

        if (recording.getStatus() != RecordingStatus.COMPLETED || recording.getStoragePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording file is not available");
        }

        Path filePath = Path.of(recording.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording file does not exist on disk");
        }

        return filePath;
    }

    private Meeting getMeeting(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));
    }

    private void verifyHost(Meeting meeting, Long userId) {
        log.info("verifyHost: meetingId = {}, hostId = {}, userId = {}", meeting.getId(), meeting.getHost().getId(), userId);
        if (!meeting.getHost().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the meeting host can perform this action. Host: " + meeting.getHost().getId() + ", User: " + userId);
        }
    }

    private void verifyParticipantOrHost(Meeting meeting, Long userId) {
        boolean isHost = meeting.getHost().getId().equals(userId);
        boolean isParticipant = participantRepository.existsByMeetingIdAndUserId(meeting.getId(), userId);
        if (!isHost && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to access this meeting");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "mp4";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Resolves the language for transcription from the meeting.
     * Currently defaults to "en". In a future phase this could read
     * a language field on the Meeting entity if added.
     */
    private String resolveLanguageFromMeeting(Meeting meeting) {
        return "en";
    }
}
