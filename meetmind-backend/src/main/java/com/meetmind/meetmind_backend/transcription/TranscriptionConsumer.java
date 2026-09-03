package com.meetmind.meetmind_backend.transcription;

import com.meetmind.meetmind_backend.event.EventIdempotencyRegistry;
import com.meetmind.meetmind_backend.event.MeetMindEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consumes RECORDING_COMPLETED events from the "recording-events" Kafka topic
 * and delegates to TranscriptionService.
 *
 * Consumer group: meetmind-transcription-group
 *
 * Retry behavior: Spring Kafka's default retry policy will retry delivery on exception.
 * If TranscriptionService.transcribe() throws (non-idempotency), the message is retried
 * up to the configured attempts, then sent to the DLT (Dead Letter Topic).
 */
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TranscriptionConsumer {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-transcription-group";

    private final TranscriptionService transcriptionService;
    private final EventIdempotencyRegistry idempotencyRegistry;

    // Stored for test inspection — accessible via getter
    private final List<MeetingTranscript> processedTranscripts = new ArrayList<>();

    public TranscriptionConsumer(TranscriptionService transcriptionService,
                                  EventIdempotencyRegistry idempotencyRegistry) {
        this.transcriptionService = transcriptionService;
        this.idempotencyRegistry = idempotencyRegistry;
    }

    @KafkaListener(
            topics = "transcription-events",
            groupId = CONSUMER_GROUP
    )
    public void consume(MeetMindEvent event) {
        if (idempotencyRegistry.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
            log.info("TranscriptionConsumer: Ignored duplicate event {}", event.getEventId());
            return;
        }

        if (!"RECORDING_READY_FOR_TRANSCRIPTION".equals(event.getEventType())) {
            log.debug("TranscriptionConsumer: Ignoring non-RECORDING_READY_FOR_TRANSCRIPTION event: {}", event.getEventType());
            return;
        }

        log.info("TranscriptionConsumer: Received RECORDING_READY_FOR_TRANSCRIPTION event={}", event.getEventId());

        Map<String, Object> metadata = event.getMetadata();
        if (metadata == null) {
            log.warn("TranscriptionConsumer: Event {} has null metadata — skipping", event.getEventId());
            return;
        }

        Long recordingId = toLong(metadata.get("recordingId"));
        Long meetingId = event.getMeetingId();
        String storagePath = (String) metadata.get("storagePath");
        String language = (String) metadata.getOrDefault("language", "en");

        if (recordingId == null || meetingId == null || storagePath == null) {
            log.error("TranscriptionConsumer: Missing required fields — recordingId={}, meetingId={}, storagePath={}",
                    recordingId, meetingId, storagePath);
            return;
        }

        try {
            MeetingTranscript transcript = transcriptionService.transcribe(recordingId, meetingId, storagePath, language);
            synchronized (processedTranscripts) {
                processedTranscripts.add(transcript);
            }
        } catch (TranscriptionService.AlreadyTranscribedException e) {
            log.info("TranscriptionConsumer: Recording {} already transcribed — skipping", recordingId);
        }
    }

    public List<MeetingTranscript> getProcessedTranscripts() {
        synchronized (processedTranscripts) {
            return new ArrayList<>(processedTranscripts);
        }
    }

    public void clear() {
        synchronized (processedTranscripts) {
            processedTranscripts.clear();
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
