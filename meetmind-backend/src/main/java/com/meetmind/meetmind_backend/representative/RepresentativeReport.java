package com.meetmind.meetmind_backend.representative;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "representative_reports")
public class RepresentativeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "representative_id", nullable = false)
    private Long representativeId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "important_discussions_json", columnDefinition = "TEXT")
    private String importantDiscussionsJson;

    @Column(name = "decisions_json", columnDefinition = "TEXT")
    private String decisionsJson;

    @Column(name = "action_items_json", columnDefinition = "TEXT")
    private String actionItemsJson;

    @Column(name = "owner_relevant_questions_json", columnDefinition = "TEXT")
    private String ownerRelevantQuestionsJson;

    @Column(name = "monitored_topics_found_json", columnDefinition = "TEXT")
    private String monitoredTopicsFoundJson;

    @Column(name = "transcript_references_json", columnDefinition = "TEXT")
    private String transcriptReferencesJson;

    @Column(name = "attendance_timeline_json", columnDefinition = "TEXT")
    private String attendanceTimelineJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RepresentativeReport() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }

    public Long getRepresentativeId() { return representativeId; }
    public void setRepresentativeId(Long representativeId) { this.representativeId = representativeId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getImportantDiscussionsJson() { return importantDiscussionsJson; }
    public void setImportantDiscussionsJson(String importantDiscussionsJson) { this.importantDiscussionsJson = importantDiscussionsJson; }

    public String getDecisionsJson() { return decisionsJson; }
    public void setDecisionsJson(String decisionsJson) { this.decisionsJson = decisionsJson; }

    public String getActionItemsJson() { return actionItemsJson; }
    public void setActionItemsJson(String actionItemsJson) { this.actionItemsJson = actionItemsJson; }

    public String getOwnerRelevantQuestionsJson() { return ownerRelevantQuestionsJson; }
    public void setOwnerRelevantQuestionsJson(String ownerRelevantQuestionsJson) { this.ownerRelevantQuestionsJson = ownerRelevantQuestionsJson; }

    public String getMonitoredTopicsFoundJson() { return monitoredTopicsFoundJson; }
    public void setMonitoredTopicsFoundJson(String monitoredTopicsFoundJson) { this.monitoredTopicsFoundJson = monitoredTopicsFoundJson; }

    public String getTranscriptReferencesJson() { return transcriptReferencesJson; }
    public void setTranscriptReferencesJson(String transcriptReferencesJson) { this.transcriptReferencesJson = transcriptReferencesJson; }

    public String getAttendanceTimelineJson() { return attendanceTimelineJson; }
    public void setAttendanceTimelineJson(String attendanceTimelineJson) { this.attendanceTimelineJson = attendanceTimelineJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
