package com.meetmind.meetmind_backend.notification;

import com.meetmind.meetmind_backend.event.SpringMeetingInvitationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationEventListener(
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @EventListener
    public void handleMeetingInvitation(SpringMeetingInvitationEvent event) {
        log.info("NotificationEventListener: Processing meeting invitation for user {} in meeting {}",
                event.getInvitedUserId(), event.getMeetingId());

        if (event.getInvitedUserId() == null) {
            return;
        }

        try {
            // 1. Create persistent in-app notification & push to user's private notification topic
            String title = "New Meeting Invitation";
            String body = event.getHostName() + " invited you to '" + event.getMeetingTitle() + "'.";
            notificationService.createNotification(
                    event.getInvitedUserId(),
                    "MEETING_INVITATION",
                    title,
                    body,
                    event.getMeetingId(),
                    null
            );

            // 2. Broadcast participant update to the meeting's real-time topic
            if (event.getMeetingId() != null) {
                Map<String, Object> meetingPayload = new HashMap<>();
                meetingPayload.put("type", "PARTICIPANT_INVITED");
                meetingPayload.put("meetingId", event.getMeetingId());
                meetingPayload.put("userId", event.getInvitedUserId());
                meetingPayload.put("title", event.getMeetingTitle());
                meetingPayload.put("hostName", event.getHostName());

                messagingTemplate.convertAndSend("/topic/meetings/" + event.getMeetingId(), meetingPayload);
            }
        } catch (Exception e) {
            log.error("NotificationEventListener: Failed to process meeting invitation: {}", e.getMessage(), e);
        }
    }
}

