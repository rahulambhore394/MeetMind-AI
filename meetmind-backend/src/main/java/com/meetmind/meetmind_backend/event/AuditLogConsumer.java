package com.meetmind.meetmind_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-audit-group";

    private final EventIdempotencyRegistry idempotencyRegistry;
    private final List<MeetMindEvent> receivedEvents = new ArrayList<>();

    public AuditLogConsumer(EventIdempotencyRegistry idempotencyRegistry) {
        this.idempotencyRegistry = idempotencyRegistry;
    }

    @KafkaListener(
            topics = {"meeting-lifecycle-events", "participant-lifecycle-events", "chat-message-events"},
            groupId = CONSUMER_GROUP
    )
    public void consume(MeetMindEvent event) {
        if (idempotencyRegistry.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
            log.info("AuditLogConsumer: Ignored duplicate event {}", event.getEventId());
            return;
        }

        try {
            if (event.getMetadata() != null && Boolean.TRUE.equals(event.getMetadata().get("fail"))) {
                log.warn("AuditLogConsumer: Simulating consumer failure for event {}", event.getEventId());
                throw new RuntimeException("Simulated consumer failure");
            }

            log.info("AUDIT LOG - EventId: {}, Type: {}, MeetingId: {}, UserId: {}, Timestamp: {}, Version: {}, Metadata: {}",
                    event.getEventId(),
                    event.getEventType(),
                    event.getMeetingId(),
                    event.getUserId(),
                    event.getTimestamp(),
                    event.getVersion(),
                    event.getMetadata()
            );

            synchronized (receivedEvents) {
                receivedEvents.add(event);
            }
        } catch (RuntimeException e) {
            idempotencyRegistry.remove(event.getEventId(), CONSUMER_GROUP);
            throw e;
        }
    }

    public List<MeetMindEvent> getReceivedEvents() {
        synchronized (receivedEvents) {
            return new ArrayList<>(receivedEvents);
        }
    }

    public void clear() {
        synchronized (receivedEvents) {
            receivedEvents.clear();
        }
    }
}
