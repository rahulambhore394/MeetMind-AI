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
