package com.meetmind.meetmind_backend.intelligence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_summaries")
public class MeetingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "transcript_id", nullable = false)
    private Long transcriptId;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_points_json", columnDefinition = "TEXT")
    private String keyPointsJson;

    @Column(name = "decisions_json", columnDefinition = "TEXT")
    private String decisionsJson;

    @Column(name = "topics_json", columnDefinition = "TEXT")
    private String topicsJson;

    @Column(name = "questions_json", columnDefinition = "TEXT")
    private String questionsJson;

    @Column(name = "analysis_json", columnDefinition = "TEXT")
    private String analysisJson;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "model_metadata", length = 200)
    private String modelMetadata;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MeetingSummary() {}

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

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public Long getTranscriptId() { return transcriptId; }
    public void setTranscriptId(Long transcriptId) { this.transcriptId = transcriptId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getKeyPointsJson() { return keyPointsJson; }
    public void setKeyPointsJson(String keyPointsJson) { this.keyPointsJson = keyPointsJson; }

    public String getDecisionsJson() { return decisionsJson; }
    public void setDecisionsJson(String decisionsJson) { this.decisionsJson = decisionsJson; }

    public String getTopicsJson() { return topicsJson; }
    public void setTopicsJson(String topicsJson) { this.topicsJson = topicsJson; }

    public String getQuestionsJson() { return questionsJson; }
    public void setQuestionsJson(String questionsJson) { this.questionsJson = questionsJson; }

    public String getAnalysisJson() { return analysisJson; }
    public void setAnalysisJson(String analysisJson) { this.analysisJson = analysisJson; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getModelMetadata() { return modelMetadata; }
    public void setModelMetadata(String modelMetadata) { this.modelMetadata = modelMetadata; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
