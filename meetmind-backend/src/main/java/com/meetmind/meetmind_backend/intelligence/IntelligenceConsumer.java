package com.meetmind.meetmind_backend.intelligence;

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
 * Consumes TRANSCRIPTION_COMPLETED events from the "transcription-events" Kafka topic
 * and triggers background AI Meeting Intelligence generation.
 *
 * Consumer group: meetmind-intelligence-group
 */
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class IntelligenceConsumer {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-intelligence-group";

    private final MeetingIntelligenceService intelligenceService;
    private final EventIdempotencyRegistry idempotencyRegistry;

    private final List<MeetingSummary> processedSummaries = new ArrayList<>();

    public IntelligenceConsumer(MeetingIntelligenceService intelligenceService,
                                EventIdempotencyRegistry idempotencyRegistry) {
        this.intelligenceService = intelligenceService;
        this.idempotencyRegistry = idempotencyRegistry;
    }

    @KafkaListener(
            topics = "transcription-events",
            groupId = CONSUMER_GROUP,
            autoStartup = "${spring.kafka.listener.auto-startup:false}"
    )
    public void consume(MeetMindEvent event) {
        if (idempotencyRegistry.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
            log.info("IntelligenceConsumer: Ignored duplicate event {}", event.getEventId());
            return;
        }

        if (!"TRANSCRIPTION_COMPLETED".equals(event.getEventType())) {
            log.debug("IntelligenceConsumer: Ignoring non-TRANSCRIPTION_COMPLETED event: {}", event.getEventType());
            return;
        }

        log.info("IntelligenceConsumer: Received TRANSCRIPTION_COMPLETED event={}", event.getEventId());

        Map<String, Object> metadata = event.getMetadata();
        if (metadata == null) {
            log.warn("IntelligenceConsumer: Event {} has null metadata — skipping", event.getEventId());
            return;
        }

        Boolean success = (Boolean) metadata.get("success");
        if (Boolean.FALSE.equals(success)) {
            log.info("IntelligenceConsumer: Transcription was marked unsuccessful — skipping AI intelligence analysis.");
            return;
        }

        Long transcriptId = toLong(metadata.get("transcriptId"));
        if (transcriptId == null) {
            log.error("IntelligenceConsumer: Missing transcriptId in event metadata — skipping");
            return;
        }

        try {
            MeetingSummary summary = intelligenceService.generateIntelligence(transcriptId);
            synchronized (processedSummaries) {
                processedSummaries.add(summary);
            }
        } catch (Exception e) {
            log.error("IntelligenceConsumer: Failed to generate intelligence for transcript {} — {}", transcriptId, e.getMessage(), e);
        }
    }

    public List<MeetingSummary> getProcessedSummaries() {
        synchronized (processedSummaries) {
            return new ArrayList<>(processedSummaries);
        }
    }

    public void clear() {
        synchronized (processedSummaries) {
            processedSummaries.clear();
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
