package com.meetmind.meetmind_backend.recording;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class RecordingProcessingService {
    private static final Logger log = LoggerFactory.getLogger(RecordingProcessingService.class);

    private final RecordingRepository recordingRepository;
    private final RecordingChunkRepository chunkRepository;
    private final AudioMetadataRepository metadataRepository;
    private final MeetingRepository meetingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public RecordingProcessingService(RecordingRepository recordingRepository, 
                                      RecordingChunkRepository chunkRepository,
                                      AudioMetadataRepository metadataRepository,
                                      MeetingRepository meetingRepository,
                                      SimpMessagingTemplate messagingTemplate,
                                      ApplicationEventPublisher eventPublisher) {
        this.recordingRepository = recordingRepository;
        this.chunkRepository = chunkRepository;
        this.metadataRepository = metadataRepository;
        this.meetingRepository = meetingRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processRecording(Long recordingId) {
        MeetingRecording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new RuntimeException("Recording not found: " + recordingId));

        log.info("Starting audio processing pipeline for recording: {}", recordingId);
        
        // 1. Validation & Audio Extraction
        updateStatus(recording, RecordingStatus.AUDIO_PREPARING);
        simulateProcessing(1000);
        saveSimulatedMetadata(recordingId);
        log.info("Recording {} validated. Extracting audio...", recordingId);

        // 2. Normalization
        updateStatus(recording, RecordingStatus.PROCESSING);
        simulateProcessing(2000);
        log.info("Audio extracted and normalized for recording {}.", recordingId);

        // 3. Chunking & Metadata Preparation
        updateStatus(recording, RecordingStatus.AUDIO_CHUNKING);
        createSimulatedChunks(recordingId, recording.getDuration());
        simulateProcessing(1500);
        log.info("Audio chunking and timestamp preparation completed for recording {}.", recordingId);

        // 4. Ready for Transcription
        updateStatus(recording, RecordingStatus.READY_FOR_TRANSCRIPTION);
        log.info("Recording {} is now READY_FOR_TRANSCRIPTION", recordingId);

        // Trigger Transcription Pipeline via Kafka
        Meeting meeting = meetingRepository.findById(recording.getMeetingId()).orElse(null);
        eventPublisher.publishEvent(new SpringRecordingReadyForTranscriptionEvent(
                this,
                recording.getId(),
                recording.getMeetingId(),
                recording.getStoragePath(),
                "en" // Default to en
        ));
    }

    private void createSimulatedChunks(Long recordingId, Long totalDurationSeconds) {
        if (totalDurationSeconds == null) totalDurationSeconds = 60L; // Default for simulation
        
        long chunkSizeMs = 30000; // 30 seconds
        long totalDurationMs = totalDurationSeconds * 1000;
        int numChunks = (int) Math.ceil((double) totalDurationMs / chunkSizeMs);

        for (int i = 0; i < numChunks; i++) {
            RecordingChunk chunk = new RecordingChunk();
            chunk.setRecordingId(recordingId);
            chunk.setChunkIndex(i);
            chunk.setStartMs(i * chunkSizeMs);
            chunk.setEndMs(Math.min((i + 1) * chunkSizeMs, totalDurationMs));
            chunk.setStatus("COMPLETED");
            chunkRepository.save(chunk);
        }
    }

    private void saveSimulatedMetadata(Long recordingId) {
        AudioMetadata metadata = new AudioMetadata();
        metadata.setRecordingId(recordingId);
        metadata.setCodec("aac");
        metadata.setSampleRate(44100);
        metadata.setChannels(2);
        metadata.setBitRate(128000L);
        metadata.setFormat("mp4");
        metadataRepository.save(metadata);
    }

    private void updateStatus(MeetingRecording recording, RecordingStatus status) {
        recording.setStatus(status);
        recordingRepository.save(recording);
        
        // Notify via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/meetings/" + recording.getMeetingId() + "/recordings",
                Map.of(
                        "type", "RECORDING_STATUS_UPDATE",
                        "recordingId", recording.getId(),
                        "status", status.name(),
                        "timestamp", System.currentTimeMillis()
                )
        );
    }

    private void simulateProcessing(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
