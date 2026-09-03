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
public class DltConsumer {

    private static final Logger log = LoggerFactory.getLogger(DltConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-dlt-group";

    private final List<MeetMindEvent> dltEvents = new ArrayList<>();

    @KafkaListener(
            topics = {
                    "meeting-lifecycle-events.DLT",
                    "participant-lifecycle-events.DLT",
                    "chat-message-events.DLT"
            },
            groupId = CONSUMER_GROUP
    )
    public void consumeDlt(MeetMindEvent event) {
        log.error("DLT ALERT - Message failed consumption retries. EventId: {}, Type: {}, MeetingId: {}",
                event.getEventId(), event.getEventType(), event.getMeetingId());
        synchronized (dltEvents) {
            dltEvents.add(event);
        }
    }

    public List<MeetMindEvent> getDltEvents() {
        synchronized (dltEvents) {
            return new ArrayList<>(dltEvents);
        }
    }

    public void clear() {
        synchronized (dltEvents) {
            dltEvents.clear();
        }
    }
}
