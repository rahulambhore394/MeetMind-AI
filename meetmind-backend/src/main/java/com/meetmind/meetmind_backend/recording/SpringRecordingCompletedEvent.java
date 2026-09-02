package com.meetmind.meetmind_backend.recording;

import org.springframework.context.ApplicationEvent;

/**
 * Published by RecordingService when an uploaded recording file reaches COMPLETED status.
 * Consumed by KafkaEventPublisher → "recording-events" Kafka topic → TranscriptionConsumer.
 */
public class SpringRecordingCompletedEvent extends ApplicationEvent {

    private final Long recordingId;
    private final Long meetingId;
    private final Long ownerId;
    private final String storagePath;
    private final String language;

    public SpringRecordingCompletedEvent(Object source,
                                         Long recordingId,
                                         Long meetingId,
                                         Long ownerId,
                                         String storagePath,
                                         String language) {
        super(source);
        this.recordingId = recordingId;
        this.meetingId = meetingId;
        this.ownerId = ownerId;
        this.storagePath = storagePath;
        this.language = language;
    }

    public Long getRecordingId() { return recordingId; }
    public Long getMeetingId() { return meetingId; }
    public Long getOwnerId() { return ownerId; }
    public String getStoragePath() { return storagePath; }
    public String getLanguage() { return language; }
}
