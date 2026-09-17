package com.meetmind.meetmind_backend.recording;

import com.meetmind.meetmind_backend.intelligence.MeetingIntelligenceService;
import com.meetmind.meetmind_backend.transcription.SpringTranscriptionCompletedEvent;
import com.meetmind.meetmind_backend.transcription.TranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LocalRecordingEventListener {

    private static final Logger log = LoggerFactory.getLogger(LocalRecordingEventListener.class);

    private final RecordingProcessingService processingService;
    private final TranscriptionService transcriptionService;
    private final MeetingIntelligenceService intelligenceService;

    @Value("${spring.kafka.enabled:false}")
    private boolean kafkaEnabled;

    public LocalRecordingEventListener(
            RecordingProcessingService processingService,
            TranscriptionService transcriptionService,
            MeetingIntelligenceService intelligenceService
    ) {
        this.processingService = processingService;
        this.transcriptionService = transcriptionService;
        this.intelligenceService = intelligenceService;
    }

    @EventListener
    public void onRecordingCompleted(SpringRecordingCompletedEvent event) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled. In-process pipeline: processing recording {}", event.getRecordingId());
            CompletableFuture.runAsync(() -> {
                try {
                    processingService.processRecording(event.getRecordingId());
                } catch (Exception e) {
                    log.error("Failed to process recording in local pipeline: {}", event.getRecordingId(), e);
                }
            });
        }
    }

    @EventListener
    public void onRecordingReadyForTranscription(SpringRecordingReadyForTranscriptionEvent event) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled. In-process pipeline: transcribing recording {}", event.getRecordingId());
            CompletableFuture.runAsync(() -> {
                try {
                    transcriptionService.transcribe(
                            event.getRecordingId(),
                            event.getMeetingId(),
                            event.getStoragePath(),
                            event.getLanguage() != null ? event.getLanguage() : "en"
                    );
                } catch (Exception e) {
                    log.error("Failed to transcribe in local pipeline for recording {}: {}", event.getRecordingId(), e.getMessage(), e);
                }
            });
        }
    }

    @EventListener
    public void onTranscriptionCompleted(SpringTranscriptionCompletedEvent event) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled. In-process pipeline: generating intelligence for transcript {}", event.getTranscriptId());
            CompletableFuture.runAsync(() -> {
                try {
                    intelligenceService.generateIntelligence(event.getTranscriptId());
                } catch (Exception e) {
                    log.error("Failed to generate intelligence in local pipeline for transcript {}: {}", event.getTranscriptId(), e.getMessage(), e);
                }
            });
        }
    }
}
