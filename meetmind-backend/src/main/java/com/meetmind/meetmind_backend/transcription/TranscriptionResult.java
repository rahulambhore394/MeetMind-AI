package com.meetmind.meetmind_backend.transcription;

import java.util.List;

/**
 * Value object (not persisted) returned by a TranscriptionProvider.
 * Each segment corresponds to a contiguous speech segment with timestamps.
 */
public class TranscriptionResult {

    private final List<SegmentData> segments;

    public TranscriptionResult(List<SegmentData> segments) {
        this.segments = segments;
    }

    public List<SegmentData> getSegments() {
        return segments;
    }

    public boolean isEmpty() {
        return segments == null || segments.isEmpty();
    }

    public static class SegmentData {
        private final String text;
        private final long startMs;
        private final long endMs;
        private final String speaker;      // nullable — populated by future diarization pipeline
        private final Double confidence;   // nullable — 0.0-1.0 if available

        public SegmentData(String text, long startMs, long endMs, String speaker, Double confidence) {
            this.text = text;
            this.startMs = startMs;
            this.endMs = endMs;
            this.speaker = speaker;
            this.confidence = confidence;
        }

        public String getText() { return text; }
        public long getStartMs() { return startMs; }
        public long getEndMs() { return endMs; }
        public String getSpeaker() { return speaker; }
        public Double getConfidence() { return confidence; }
    }
}
