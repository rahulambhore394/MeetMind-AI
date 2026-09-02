package com.meetmind.meetmind_backend.recording;

import com.meetmind.meetmind_backend.event.EventIdempotencyRegistry;
import com.meetmind.meetmind_backend.event.MeetMindEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RecordingProcessingConsumer {

    private static final Logger log = LoggerFactory.getLogger(RecordingProcessingConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-recording-processing-group";

    private final RecordingProcessingService processingService;
    private final EventIdempotencyRegistry idempotencyRegistry;

    public RecordingProcessingConsumer(RecordingProcessingService processingService, EventIdempotencyRegistry idempotencyRegistry) {
        this.processingService = processingService;
        this.idempotencyRegistry = idempotencyRegistry;
    }

    @KafkaListener(
            topics = "recording-events",
            groupId = CONSUMER_GROUP
    )
    public void consume(MeetMindEvent event) {
        if (idempotencyRegistry.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
            log.info("RecordingProcessingConsumer: Ignored duplicate event {}", event.getEventId());
            return;
        }

        if (!"RECORDING_COMPLETED".equals(event.getEventType())) {
            return;
        }

        Map<String, Object> metadata = event.getMetadata();
        if (metadata == null) return;

        Long recordingId = toLong(metadata.get("recordingId"));
        if (recordingId == null) return;

        log.info("RecordingProcessingConsumer: Starting processing for recordingId={}", recordingId);
        try {
            processingService.processRecording(recordingId);
        } catch (Exception e) {
            log.error("RecordingProcessingConsumer: Failed to process recording {}", recordingId, e);
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
