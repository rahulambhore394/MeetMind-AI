package com.meetmind.meetmind_backend.intelligence;

import com.meetmind.meetmind_backend.event.MeetMindEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class IntelligenceWebSocketConsumer {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceWebSocketConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-intelligence-ws-group";

    private final SimpMessagingTemplate messagingTemplate;

    public IntelligenceWebSocketConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
            topics = "intelligence-events",
            groupId = CONSUMER_GROUP
    )
    public void consume(MeetMindEvent event) {
        if (!"AI_SUMMARY_READY".equals(event.getEventType())) {
            return;
        }

        log.info("IntelligenceWebSocketConsumer: Broadcasting AI_SUMMARY_READY for meeting {}", event.getMeetingId());

        messagingTemplate.convertAndSend(
                "/topic/meetings/" + event.getMeetingId() + "/intelligence",
                Map.of(
                        "type", "AI_SUMMARY_READY",
                        "meetingId", event.getMeetingId(),
                        "timestamp", System.currentTimeMillis()
                )
        );
    }
}
