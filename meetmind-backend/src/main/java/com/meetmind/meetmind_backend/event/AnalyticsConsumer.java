package com.meetmind.meetmind_backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-analytics-group";

    private final EventIdempotencyRegistry idempotencyRegistry;

    private final AtomicInteger meetingStartedCount = new AtomicInteger(0);
    private final AtomicInteger meetingEndedCount = new AtomicInteger(0);
    private final AtomicInteger participantJoinedCount = new AtomicInteger(0);
    private final AtomicInteger participantLeftCount = new AtomicInteger(0);
    private final AtomicInteger chatMessageCount = new AtomicInteger(0);

    public AnalyticsConsumer(EventIdempotencyRegistry idempotencyRegistry) {
        this.idempotencyRegistry = idempotencyRegistry;
    }

    @KafkaListener(
            topics = {"meeting-lifecycle-events", "participant-lifecycle-events", "chat-message-events"},
            groupId = CONSUMER_GROUP,
            autoStartup = "${spring.kafka.listener.auto-startup:false}"
    )
    public void consume(MeetMindEvent event) {
        if (idempotencyRegistry.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
            log.info("AnalyticsConsumer: Ignored duplicate event {}", event.getEventId());
            return;
        }

        log.info("ANALYTICS ENGINE - Processing event: {} of type {}", event.getEventId(), event.getEventType());

        switch (event.getEventType()) {
            case "MEETING_STARTED":
                meetingStartedCount.incrementAndGet();
                break;
            case "MEETING_ENDED":
                meetingEndedCount.incrementAndGet();
                break;
            case "PARTICIPANT_JOINED":
                participantJoinedCount.incrementAndGet();
                break;
            case "PARTICIPANT_LEFT":
                participantLeftCount.incrementAndGet();
                break;
            case "CHAT_MESSAGE_SENT":
                chatMessageCount.incrementAndGet();
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    public int getMeetingStartedCount() {
        return meetingStartedCount.get();
    }

    public int getMeetingEndedCount() {
        return meetingEndedCount.get();
    }

    public int getParticipantJoinedCount() {
        return participantJoinedCount.get();
    }

    public int getParticipantLeftCount() {
        return participantLeftCount.get();
    }

    public int getChatMessageCount() {
        return chatMessageCount.get();
    }

    public void reset() {
        meetingStartedCount.set(0);
        meetingEndedCount.set(0);
        participantJoinedCount.set(0);
        participantLeftCount.set(0);
        chatMessageCount.set(0);
    }
}
