package com.meetmind.meetmind_backend.notification;

import com.meetmind.meetmind_backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotifications(user.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, @AuthenticationPrincipal User user) {
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Void> clearAllNotifications(@AuthenticationPrincipal User user) {
        notificationService.clearAllNotifications(user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Notification> createTestNotification(
            @RequestParam(required = false, defaultValue = "AI_SUMMARY_READY") String type,
            @AuthenticationPrincipal User user
    ) {
        String title = "AI_SUMMARY_READY".equals(type) ? "AI Meeting Summary Ready" : "New Meeting Invitation";
        String body = "AI_SUMMARY_READY".equals(type) 
                ? "Your recent strategy call has been summarized with 5 key decisions and 3 action items."
                : "Alex Chen invited you to 'Quarterly Product Review'.";
        Notification notification = notificationService.createNotification(
                user.getId(),
                type,
                title,
                body,
                null,
                null
        );
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreference> getPreferences(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getPreferences(user.getId()));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @RequestBody NotificationPreference prefs,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificationService.updatePreferences(user.getId(), prefs));
    }

    @PostMapping("/devices/register")
    public ResponseEntity<Void> registerDevice(@RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken != null) {
            notificationService.registerDevice(user.getId(), fcmToken);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/devices/unregister")
    public ResponseEntity<Void> unregisterDevice(@RequestBody Map<String, String> body) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken != null) {
            notificationService.unregisterDevice(fcmToken);
        }
        return ResponseEntity.ok().build();
    }
}
