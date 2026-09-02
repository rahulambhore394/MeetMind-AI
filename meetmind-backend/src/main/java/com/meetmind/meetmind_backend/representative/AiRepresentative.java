package com.meetmind.meetmind_backend.representative;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_representatives")
public class AiRepresentative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RepresentativeStatus status;

    @Column(name = "media_storage_path", length = 500)
    private String mediaStoragePath;

    @Column(name = "monitored_topics_json", columnDefinition = "TEXT")
    private String monitoredTopicsJson;

    @Column(name = "monitored_questions_json", columnDefinition = "TEXT")
    private String monitoredQuestionsJson;

    @Column(name = "important_people_json", columnDefinition = "TEXT")
    private String importantPeopleJson;

    @Column(name = "report_preferences_json", columnDefinition = "TEXT")
    private String reportPreferencesJson;

    @Column(name = "notification_preferences_json", columnDefinition = "TEXT")
    private String notificationPreferencesJson;

    @Column(name = "consent_disclosure", nullable = false)
    private Boolean consentDisclosure = true;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AiRepresentative() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public RepresentativeStatus getStatus() { return status; }
    public void setStatus(RepresentativeStatus status) { this.status = status; }

    public String getMediaStoragePath() { return mediaStoragePath; }
    public void setMediaStoragePath(String mediaStoragePath) { this.mediaStoragePath = mediaStoragePath; }

    public String getMonitoredTopicsJson() { return monitoredTopicsJson; }
    public void setMonitoredTopicsJson(String monitoredTopicsJson) { this.monitoredTopicsJson = monitoredTopicsJson; }

    public String getMonitoredQuestionsJson() { return monitoredQuestionsJson; }
    public void setMonitoredQuestionsJson(String monitoredQuestionsJson) { this.monitoredQuestionsJson = monitoredQuestionsJson; }

    public String getImportantPeopleJson() { return importantPeopleJson; }
    public void setImportantPeopleJson(String importantPeopleJson) { this.importantPeopleJson = importantPeopleJson; }

    public String getReportPreferencesJson() { return reportPreferencesJson; }
    public void setReportPreferencesJson(String reportPreferencesJson) { this.reportPreferencesJson = reportPreferencesJson; }

    public String getNotificationPreferencesJson() { return notificationPreferencesJson; }
    public void setNotificationPreferencesJson(String notificationPreferencesJson) { this.notificationPreferencesJson = notificationPreferencesJson; }

    public Boolean getConsentDisclosure() { return consentDisclosure; }
    public void setConsentDisclosure(Boolean consentDisclosure) { this.consentDisclosure = consentDisclosure; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
