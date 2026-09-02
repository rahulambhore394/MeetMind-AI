package com.meetmind.meetmind_backend.recording;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recording_chunks")
public class RecordingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recording_id", nullable = false)
    private Long recordingId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "start_ms", nullable = false)
    private Long startMs;

    @Column(name = "end_ms", nullable = false)
    private Long endMs;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecordingChunk() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public Long getStartMs() { return startMs; }
    public void setStartMs(Long startMs) { this.startMs = startMs; }
    public Long getEndMs() { return endMs; }
    public void setEndMs(Long endMs) { this.endMs = endMs; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
