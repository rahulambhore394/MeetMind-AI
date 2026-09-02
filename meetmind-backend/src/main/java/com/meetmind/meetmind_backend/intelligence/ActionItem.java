package com.meetmind.meetmind_backend.intelligence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_action_items")
public class ActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "meeting_summary_id", nullable = false)
    private Long meetingSummaryId;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "assigned_user")
    private String assignedUser;

    @Column(name = "due_date")
    private String dueDate;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ActionItemStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ActionItem() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public Long getMeetingSummaryId() { return meetingSummaryId; }
    public void setMeetingSummaryId(Long meetingSummaryId) { this.meetingSummaryId = meetingSummaryId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedUser() { return assignedUser; }
    public void setAssignedUser(String assignedUser) { this.assignedUser = assignedUser; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public ActionItemStatus getStatus() { return status; }
    public void setStatus(ActionItemStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
