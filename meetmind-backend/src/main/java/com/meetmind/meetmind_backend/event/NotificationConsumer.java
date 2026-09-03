package com.meetmind.meetmind_backend.event;

import com.meetmind.meetmind_backend.notification.NotificationService;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.participant.ParticipantRepository;
import com.meetmind.meetmind_backend.user.User;
import com.meetmind.meetmind_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

import org.slf4j.MDC;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String CONSUMER_GROUP = "meetmind-notification-group";

    private final EventIdempotencyRegistry idempotencyRegistry;
    private final NotificationService notificationService;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public NotificationConsumer(
            EventIdempotencyRegistry idempotencyRegistry,
            NotificationService notificationService,
            ParticipantRepository participantRepository,
            UserRepository userRepository
    ) {
        this.idempotencyRegistry = idempotencyRegistry;
        this.notificationService = notificationService;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @KafkaListener(
            topics = {
                    "meeting-lifecycle-events",
                    "participant-lifecycle-events",
                    "recording-events",
                    "transcription-events",
                    "intelligence-events",
                    "representative-events"
            },
            groupId = CONSUMER_GROUP
    )
    public void consume(MeetMindEvent event) {
        String requestId = (String) event.getMetadata().get("requestId");
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        
        try {
            if (idempotencyRegistry.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
                log.info("NotificationConsumer: Ignored duplicate event {}", event.getEventId());
                return;
            }

            log.info("NOTIFICATION ENGINE - Processing event: {} type: {}", event.getEventId(), event.getEventType());

            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process notification for event {}", event.getEventId(), e);
            }
        } finally {
            MDC.remove("requestId");
        }
    }

    private void processEvent(MeetMindEvent event) {
        String type = event.getEventType();
        Long meetingId = event.getMeetingId();
        
        switch (type) {
            case "MEETING_INVITATION":
                if (event.getUserId() != null) {
                    notificationService.createNotification(event.getUserId(), "MEETING_INVITATION",
                        "New Meeting Invitation",
                        event.getMetadata().get("hostName") + " invited you to '" + event.getMetadata().get("meetingTitle") + "'.",
                        meetingId, null);
                }
                break;

            case "MEETING_STARTED":
                notifyAllParticipants(meetingId, "MEETING_STARTED", 
                    "Meeting Started", 
                    "The meeting '" + event.getMetadata().get("title") + "' has started.");
                break;
                
            case "CHAT_MESSAGE_SENT":
                // Notify participants except the sender
                notifyOtherParticipants(meetingId, event.getUserId(), "NEW_MESSAGE",
                    "New Message",
                    event.getMetadata().get("senderName") + ": " + event.getMetadata().get("message"));
                break;

            case "TRANSCRIPTION_COMPLETED":
                notifyAllParticipants(meetingId, "TRANSCRIPT_READY", 
                    "Transcript Ready", 
                    "The transcript for your recent meeting is now available.");
                break;
                
            case "AI_SUMMARY_READY":
                notifyAllParticipants(meetingId, "AI_SUMMARY_READY", 
                    "AI Summary Ready", 
                    "Meeting intelligence and summary are ready for review.");
                break;

            case "ACTION_ITEM_ASSIGNED":
                String assignedUser = (String) event.getMetadata().get("assignedUser");
                if (assignedUser != null) {
                    userRepository.findByEmail(assignedUser).ifPresent(user -> {
                        notificationService.createNotification(user.getId(), "ACTION_ITEM_ASSIGNED",
                            "New Action Item",
                            "You've been assigned: " + event.getMetadata().get("description"),
                            meetingId, null);
                    });
                }
                break;
                
            case "REPRESENTATIVE_ACTIVATED":
                if (event.getUserId() != null) {
                    notificationService.createNotification(event.getUserId(), "REPRESENTATIVE_ACTIVE",
                        "Representative Joined",
                        "Your AI Representative has joined the meeting.",
                        meetingId, (Long) event.getMetadata().get("representativeId"));
                }
                break;
                
            case "REPRESENTATIVE_REPORT_READY":
                if (event.getUserId() != null) {
                    notificationService.createNotification(event.getUserId(), "REPRESENTATIVE_REPORT_READY",
                        "Representative Report Ready",
                        "The report from your AI Representative is ready.",
                        meetingId, (Long) event.getMetadata().get("representativeId"));
                }
                break;

            case "PARTICIPANT_JOINED":
                // Maybe notify only host? For now skip to avoid spam
                break;
                
            default:
                log.info("No notification rule for event type: {}", type);
        }
    }

    private void notifyAllParticipants(Long meetingId, String type, String title, String body) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
        for (MeetingParticipant p : participants) {
            notificationService.createNotification(p.getUser().getId(), type, title, body, meetingId, null);
        }
    }

    private void notifyOtherParticipants(Long meetingId, Long senderId, String type, String title, String body) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
        for (MeetingParticipant p : participants) {
            if (!p.getUser().getId().equals(senderId)) {
                notificationService.createNotification(p.getUser().getId(), type, title, body, meetingId, null);
            }
        }
    }
}
