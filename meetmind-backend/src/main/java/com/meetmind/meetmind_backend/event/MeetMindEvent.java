package com.meetmind.meetmind_backend.event;

import java.io.Serializable;
import java.util.Map;

public class MeetMindEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private long timestamp;
    private int version;
    private Long meetingId;
    private Long userId;
    private Map<String, Object> metadata;

    public MeetMindEvent() {
    }

    public MeetMindEvent(
            String eventId,
            String eventType,
            long timestamp,
            int version,
            Long meetingId,
            Long userId,
            Map<String, Object> metadata
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.version = version;
        this.meetingId = meetingId;
        this.userId = userId;
        this.metadata = metadata;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
