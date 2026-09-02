package com.meetmind.meetmind_backend.transcription;

import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.recording.MeetingRecording;
import com.meetmind.meetmind_backend.recording.RecordingRepository;
import com.meetmind.meetmind_backend.recording.RecordingStatus;
import com.meetmind.meetmind_backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);

    private final TranscriptionProvider provider;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final RecordingRepository recordingRepository;
    private final MeetingRepository meetingRepository;
    private final ParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public TranscriptionService(
            TranscriptionProvider provider,
            TranscriptRepository transcriptRepository,
            TranscriptSegmentRepository segmentRepository,
            RecordingRepository recordingRepository,
            MeetingRepository meetingRepository,
            ParticipantRepository participantRepository,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.provider = provider;
        this.transcriptRepository = transcriptRepository;
        this.segmentRepository = segmentRepository;
        this.recordingRepository = recordingRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Entry point called by TranscriptionConsumer.
     * Idempotent: if a COMPLETED transcript already exists for this recordingId, skip.
     */
    @Transactional(noRollbackFor = TranscriptionService.AlreadyTranscribedException.class)
    public MeetingTranscript transcribe(Long recordingId, Long meetingId, String storagePath, String language) {

        // Idempotency: skip if already successfully transcribed
        transcriptRepository.findByRecordingId(recordingId).ifPresent(existing -> {
            if (existing.getStatus() == TranscriptionStatus.COMPLETED) {
                log.info("TranscriptionService: recording {} already transcribed (transcript {}). Skipping.",
                        recordingId, existing.getId());
                throw new AlreadyTranscribedException(existing);
            }
        });

        // Resolve or create the transcript record
        MeetingTranscript transcript = transcriptRepository.findByRecordingId(recordingId)
                .orElseGet(() -> {
                    MeetingTranscript t = new MeetingTranscript();
                    t.setMeetingId(meetingId);
                    t.setRecordingId(recordingId);
                    t.setLanguage(language != null ? language : "en");
                    t.setStatus(TranscriptionStatus.PENDING);
                    t.setProviderName(provider.providerName());
                    return transcriptRepository.save(t);
                });

        transcript.setStatus(TranscriptionStatus.PROCESSING);
        transcript.setProviderName(provider.providerName());
        transcript = transcriptRepository.save(transcript);

        notifyStatusUpdate(transcript.getRecordingId(), transcript.getMeetingId(), RecordingStatus.TRANSCRIPTION_PROCESSING);

        log.info("TranscriptionService: Starting transcription — recordingId={}, meetingId={}, language={}, provider={}",
                recordingId, meetingId, language, provider.providerName());

        // Validate audio file
        if (storagePath == null || storagePath.isBlank()) {
            return fail(transcript, "Storage path is null or empty");
        }

        Path audioPath = Path.of(storagePath);
        if (!Files.exists(audioPath)) {
            return fail(transcript, "Audio file not found on disk: " + storagePath);
        }

        if (Files.isDirectory(audioPath)) {
            return fail(transcript, "Storage path points to a directory, not a file: " + storagePath);
        }

        try {
            long fileSize = Files.size(audioPath);
            if (fileSize == 0) {
                return fail(transcript, "Audio file is empty (0 bytes): " + storagePath);
            }
        } catch (Exception e) {
            return fail(transcript, "Cannot read file size: " + e.getMessage());
        }

        // Run transcription
        try {
            TranscriptionResult result = provider.transcribe(audioPath, language != null ? language : "en");
            return persistSuccess(transcript, result);
        } catch (TranscriptionException e) {
            log.error("TranscriptionService: Provider failure for recording {} — {}", recordingId, e.getMessage());
            return fail(transcript, e.getMessage());
        }
    }

    private MeetingTranscript persistSuccess(MeetingTranscript transcript, TranscriptionResult result) {
        // Delete any stale segments from a previous failed attempt
        segmentRepository.deleteByTranscriptId(transcript.getId());

        List<TranscriptSegment> savedSegments = new ArrayList<>();
        List<TranscriptionResult.SegmentData> resultSegments = result.getSegments();

        for (int i = 0; i < resultSegments.size(); i++) {
            TranscriptionResult.SegmentData data = resultSegments.get(i);
            TranscriptSegment segment = new TranscriptSegment();
            segment.setTranscriptId(transcript.getId());
            segment.setSegmentIndex(i);
            segment.setText(data.getText());
            segment.setStartMs(data.getStartMs());
            segment.setEndMs(data.getEndMs());
            segment.setSpeaker(data.getSpeaker());
            segment.setConfidence(data.getConfidence());
            savedSegments.add(segmentRepository.save(segment));
        }

        // Assemble full text
        StringBuilder fullText = new StringBuilder();
        for (TranscriptSegment s : savedSegments) {
            if (!fullText.isEmpty()) fullText.append(" ");
            fullText.append(s.getText());
        }

        transcript.setStatus(TranscriptionStatus.COMPLETED);
        transcript.setFullText(fullText.toString().trim());
        transcript.setCompletedAt(LocalDateTime.now());
        transcript = transcriptRepository.save(transcript);

        notifyStatusUpdate(transcript.getRecordingId(), transcript.getMeetingId(), RecordingStatus.COMPLETED);

        log.info("TranscriptionService: Transcription COMPLETED — transcriptId={}, segments={}, chars={}",
                transcript.getId(), savedSegments.size(), fullText.length());

        // Publish completion event for downstream consumers (Kafka)
        eventPublisher.publishEvent(new SpringTranscriptionCompletedEvent(
                this,
                transcript.getId(),
                transcript.getMeetingId(),
                transcript.getRecordingId(),
                savedSegments.size(),
                transcript.getLanguage(),
                true
        ));

        return transcript;
    }

    private MeetingTranscript fail(MeetingTranscript transcript, String reason) {
        transcript.setStatus(TranscriptionStatus.FAILED);
        transcript.setErrorMessage(reason);
        transcript.setCompletedAt(LocalDateTime.now());
        transcript = transcriptRepository.save(transcript);

        notifyStatusUpdate(transcript.getRecordingId(), transcript.getMeetingId(), RecordingStatus.FAILED);

        log.warn("TranscriptionService: Transcription FAILED — transcriptId={}, reason={}", transcript.getId(), reason);

        eventPublisher.publishEvent(new SpringTranscriptionCompletedEvent(
                this,
                transcript.getId(),
                transcript.getMeetingId(),
                transcript.getRecordingId(),
                0,
                transcript.getLanguage(),
                false
        ));

        return transcript;
    }

    private void notifyStatusUpdate(Long recordingId, Long meetingId, RecordingStatus status) {
        messagingTemplate.convertAndSend(
                "/topic/meetings/" + meetingId + "/recordings",
                Map.of(
                        "type", "RECORDING_STATUS_UPDATE",
                        "recordingId", recordingId,
                        "meetingId", meetingId,
                        "status", status.name(),
                        "timestamp", System.currentTimeMillis()
                )
        );
    }

    // --- Read operations ---

    public List<MeetingTranscript> listTranscripts(Long meetingId, User user) {
        verifyAccess(meetingId, user.getId());
        return transcriptRepository.findByMeetingId(meetingId);
    }

    public TranscriptDetailView getTranscript(Long meetingId, Long transcriptId, User user) {
        verifyAccess(meetingId, user.getId());

        MeetingTranscript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript not found"));

        if (!transcript.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transcript does not belong to this meeting");
        }

        List<TranscriptSegment> segments = segmentRepository.findByTranscriptIdOrderBySegmentIndex(transcriptId);
        return new TranscriptDetailView(transcript, segments);
    }

    /**
     * Manually trigger transcription for a recording (host only).
     * Used for reprocessing or when the automatic Kafka trigger was missed.
     */
    public MeetingTranscript triggerTranscription(Long meetingId, Long recordingId, String language, User user) {
        com.meetmind.meetmind_backend.meeting.Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meeting not found"));

        if (!meeting.getHost().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the meeting host can trigger transcription");
        }

        MeetingRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recording not found"));

        if (!recording.getMeetingId().equals(meetingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording does not belong to this meeting");
        }

        return transcribe(recordingId, meetingId, recording.getStoragePath(),
                language != null ? language : "en");
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

    /**
     * Internal sentinel used to break out of the transcribe() flow cleanly when
     * idempotency check passes — not propagated to callers.
     */
    static class AlreadyTranscribedException extends RuntimeException {
        private final MeetingTranscript transcript;
        AlreadyTranscribedException(MeetingTranscript t) {
            super("Already transcribed");
            this.transcript = t;
        }
        MeetingTranscript getTranscript() { return transcript; }
    }

    /**
     * Combined view object for API responses — transcript + ordered segments.
     */
    public record TranscriptDetailView(MeetingTranscript transcript, List<TranscriptSegment> segments) {}
}
