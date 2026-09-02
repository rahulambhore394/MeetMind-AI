package com.meetmind.meetmind_backend.event;

import org.springframework.context.ApplicationEvent;

public class SpringMeetingEndedEvent extends ApplicationEvent {
    private final Long meetingId;
    private final Long hostId;
    private final String hostName;

    public SpringMeetingEndedEvent(Object source, Long meetingId, Long hostId, String hostName) {
        super(source);
        this.meetingId = meetingId;
        this.hostId = hostId;
        this.hostName = hostName;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }
}
