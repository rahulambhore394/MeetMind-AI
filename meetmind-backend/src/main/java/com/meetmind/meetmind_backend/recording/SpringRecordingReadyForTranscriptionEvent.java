package com.meetmind.meetmind_backend.recording;

import org.springframework.context.ApplicationEvent;

public class SpringRecordingReadyForTranscriptionEvent extends ApplicationEvent {
    private final Long recordingId;
    private final Long meetingId;
    private final String storagePath;
    private final String language;

    public SpringRecordingReadyForTranscriptionEvent(Object source, Long recordingId, Long meetingId, String storagePath, String language) {
        super(source);
        this.recordingId = recordingId;
        this.meetingId = meetingId;
        this.storagePath = storagePath;
        this.language = language;
    }

    public Long getRecordingId() { return recordingId; }
    public Long getMeetingId() { return meetingId; }
    public String getStoragePath() { return storagePath; }
    public String getLanguage() { return language; }
}
