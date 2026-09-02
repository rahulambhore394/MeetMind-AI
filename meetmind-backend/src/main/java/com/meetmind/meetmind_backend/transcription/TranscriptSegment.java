package com.meetmind.meetmind_backend.transcription;

import jakarta.persistence.*;

/**
 * A single timestamped speech segment within a transcript.
 * Designed for future processing by transcription, summary, and analysis pipelines.
 *
 * Fields:
 *   - startMs / endMs : milliseconds from the start of the audio file
 *   - speaker         : nullable — populated by future diarization pipeline
 *   - confidence      : nullable — model confidence 0.0-1.0
 */
@Entity
@Table(name = "transcript_segments", indexes = {
        @Index(name = "idx_segment_transcript_id", columnList = "transcript_id")
})
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transcript_id", nullable = false)
    private Long transcriptId;

    @Column(name = "segment_index", nullable = false)
    private int segmentIndex;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "start_ms", nullable = false)
    private long startMs;

    @Column(name = "end_ms", nullable = false)
    private long endMs;

    /**
     * Speaker label (e.g. "SPEAKER_00") — null until diarization is added.
     */
    @Column(name = "speaker", length = 50)
    private String speaker;

    /**
     * Model confidence for this segment (0.0 to 1.0) — null if not provided by the model.
     */
    @Column(name = "confidence")
    private Double confidence;

    public TranscriptSegment() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public Long getTranscriptId() { return transcriptId; }
    public void setTranscriptId(Long transcriptId) { this.transcriptId = transcriptId; }

    public int getSegmentIndex() { return segmentIndex; }
    public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getStartMs() { return startMs; }
    public void setStartMs(long startMs) { this.startMs = startMs; }

    public long getEndMs() { return endMs; }
    public void setEndMs(long endMs) { this.endMs = endMs; }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
