package com.meetmind.meetmind_backend.notification;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "meeting_reminders", nullable = false)
    private boolean meetingReminders = true;

    @Column(name = "meeting_started", nullable = false)
    private boolean meetingStarted = true;

    @Column(name = "chat_messages", nullable = false)
    private boolean chatMessages = true;

    @Column(name = "ai_insights", nullable = false)
    private boolean aiInsights = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    public NotificationPreference() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isMeetingReminders() { return meetingReminders; }
    public void setMeetingReminders(boolean meetingReminders) { this.meetingReminders = meetingReminders; }
    public boolean isMeetingStarted() { return meetingStarted; }
    public void setMeetingStarted(boolean meetingStarted) { this.meetingStarted = meetingStarted; }
    public boolean isChatMessages() { return chatMessages; }
    public void setChatMessages(boolean chatMessages) { this.chatMessages = chatMessages; }
    public boolean isAiInsights() { return aiInsights; }
    public void setAiInsights(boolean aiInsights) { this.aiInsights = aiInsights; }
    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }
}
