package com.meetmind.meetmind_backend.event;

import org.springframework.context.ApplicationEvent;

public class SpringMeetingStartedEvent extends ApplicationEvent {
    private final Long meetingId;
    private final Long hostId;
    private final String hostName;
    private final String title;

    public SpringMeetingStartedEvent(Object source, Long meetingId, Long hostId, String hostName, String title) {
        super(source);
        this.meetingId = meetingId;
        this.hostId = hostId;
        this.hostName = hostName;
        this.title = title;
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

    public String getTitle() {
        return title;
    }
}
