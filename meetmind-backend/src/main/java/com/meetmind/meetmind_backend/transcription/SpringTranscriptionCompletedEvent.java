package com.meetmind.meetmind_backend.transcription;

import org.springframework.context.ApplicationEvent;

/**
 * Published by TranscriptionService when transcription finishes (COMPLETED or FAILED).
 * Consumed by KafkaEventPublisher → "transcription-events" Kafka topic.
 */
public class SpringTranscriptionCompletedEvent extends ApplicationEvent {

    private final Long transcriptId;
    private final Long meetingId;
    private final Long recordingId;
    private final int segmentCount;
    private final String language;
    private final boolean success;

    public SpringTranscriptionCompletedEvent(Object source,
                                              Long transcriptId,
                                              Long meetingId,
                                              Long recordingId,
                                              int segmentCount,
                                              String language,
                                              boolean success) {
        super(source);
        this.transcriptId = transcriptId;
        this.meetingId = meetingId;
        this.recordingId = recordingId;
        this.segmentCount = segmentCount;
        this.language = language;
        this.success = success;
    }

    public Long getTranscriptId() { return transcriptId; }
    public Long getMeetingId() { return meetingId; }
    public Long getRecordingId() { return recordingId; }
    public int getSegmentCount() { return segmentCount; }
    public String getLanguage() { return language; }
    public boolean isSuccess() { return success; }
}
