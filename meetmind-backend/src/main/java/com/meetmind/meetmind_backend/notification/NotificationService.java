package com.meetmind.meetmind_backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserDeviceRepository userDeviceRepository,
            NotificationPreferenceRepository preferenceRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.preferenceRepository = preferenceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification createNotification(Long userId, String type, String title, String body, Long meetingId, Long representativeId) {
        NotificationPreference prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference p = new NotificationPreference();
                    p.setUserId(userId);
                    return preferenceRepository.save(p);
                });

        if (!shouldSend(type, prefs)) {
            log.info("Notification of type {} disabled for user {}, skipping", type, userId);
            return null;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRelatedMeetingId(meetingId);
        notification.setRelatedRepresentativeId(representativeId);
        notification.setRead(false);

        notification = notificationRepository.save(notification);

        log.info("Created notification {} for user {}", notification.getId(), userId);

        // Deliver via WebSocket
        sendToWebSocket(notification);

        // Deliver via FCM (Placeholder)
        sendPushNotification(notification);

        return notification;
    }

    private boolean shouldSend(String type, NotificationPreference prefs) {
        switch (type) {
            case "MEETING_INVITATION":
            case "MEETING_REMINDER": return prefs.isMeetingReminders();
            case "MEETING_STARTED": return prefs.isMeetingStarted();
            case "NEW_MESSAGE": return prefs.isChatMessages();
            case "TRANSCRIPT_READY":
            case "AI_SUMMARY_READY":
            case "ACTION_ITEM_ASSIGNED":
            case "REPRESENTATIVE_ACTIVE":
            case "REPRESENTATIVE_REPORT_READY": return prefs.isAiInsights();
            default: return true;
        }
    }

    private void sendToWebSocket(Notification notification) {
        String destination = "/topic/users/" + notification.getUserId() + "/notifications";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("type", notification.getType());
        payload.put("title", notification.getTitle());
        payload.put("body", notification.getBody());
        payload.put("meetingId", notification.getRelatedMeetingId());
        payload.put("representativeId", notification.getRelatedRepresentativeId());
        payload.put("createdAt", notification.getCreatedAt().toString());

        messagingTemplate.convertAndSend(destination, payload);
    }

    private void sendPushNotification(Notification notification) {
        NotificationPreference prefs = preferenceRepository.findByUserId(notification.getUserId()).orElse(null);
        if (prefs != null && !prefs.isPushEnabled()) {
            log.info("Push notifications disabled for user {}, skipping", notification.getUserId());
            return;
        }

        List<UserDevice> devices = userDeviceRepository.findByUserId(notification.getUserId());
        if (devices.isEmpty()) {
            log.info("No registered devices for user {}, skipping push notification", notification.getUserId());
            return;
        }

        for (UserDevice device : devices) {
            log.info("PUSH NOTIFICATION (SIMULATED) to device {}: {} - {}", 
                device.getFcmToken(), notification.getTitle(), notification.getBody());
            // Here you would use FirebaseAdmin SDK to send the message
        }
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Notification n : unread) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
            }
        }
        notificationRepository.saveAll(unread);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void registerDevice(Long userId, String fcmToken) {
        userDeviceRepository.findByUserIdAndFcmToken(userId, fcmToken)
                .ifPresentOrElse(
                        device -> {
                            device.setUpdatedAt(LocalDateTime.now());
                            userDeviceRepository.save(device);
                        },
                        () -> {
                            UserDevice device = new UserDevice();
                            device.setUserId(userId);
                            device.setFcmToken(fcmToken);
                            userDeviceRepository.save(device);
                        }
                );
    }

    @Transactional
    public void unregisterDevice(String fcmToken) {
        userDeviceRepository.deleteByFcmToken(fcmToken);
    }

    public NotificationPreference getPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference p = new NotificationPreference();
                    p.setUserId(userId);
                    return preferenceRepository.save(p);
                });
    }

    @Transactional
    public NotificationPreference updatePreferences(Long userId, NotificationPreference newPrefs) {
        NotificationPreference existing = getPreferences(userId);
        existing.setMeetingReminders(newPrefs.isMeetingReminders());
        existing.setMeetingStarted(newPrefs.isMeetingStarted());
        existing.setChatMessages(newPrefs.isChatMessages());
        existing.setAiInsights(newPrefs.isAiInsights());
        existing.setPushEnabled(newPrefs.isPushEnabled());
        return preferenceRepository.save(existing);
    }
}
