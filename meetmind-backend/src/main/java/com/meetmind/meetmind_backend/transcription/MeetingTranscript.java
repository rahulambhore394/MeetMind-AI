package com.meetmind.meetmind_backend.transcription;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persists the overall transcript for a recording.
 * Each recording produces at most one MeetingTranscript.
 * Segments are stored separately in transcript_segments.
 */
@Entity
@Table(name = "transcripts")
public class MeetingTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "recording_id", nullable = true)
    private Long recordingId;

    /**
     * BCP-47 language code. E.g. "en", "hi", "mr".
     */
    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscriptionStatus status;

    /**
     * Full assembled text — concatenation of all segments separated by spaces.
     * Null until transcription is COMPLETED.
     */
    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    /**
     * Populated on FAILED status — describes what went wrong.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Name of the provider that produced this transcript.
     * Preserved for audit and future re-processing with a better provider.
     */
    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public MeetingTranscript() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public TranscriptionStatus getStatus() { return status; }
    public void setStatus(TranscriptionStatus status) { this.status = status; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
