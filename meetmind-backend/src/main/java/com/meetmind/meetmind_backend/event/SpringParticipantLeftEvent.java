package com.meetmind.meetmind_backend.event;

import org.springframework.context.ApplicationEvent;

public class SpringParticipantLeftEvent extends ApplicationEvent {
    private final Long meetingId;
    private final Long userId;
    private final String userName;

    public SpringParticipantLeftEvent(Object source, Long meetingId, Long userId, String userName) {
        super(source);
        this.meetingId = meetingId;
        this.userId = userId;
        this.userName = userName;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}
