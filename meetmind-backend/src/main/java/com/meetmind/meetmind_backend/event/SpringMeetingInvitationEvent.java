package com.meetmind.meetmind_backend.event;

import org.springframework.context.ApplicationEvent;

public class SpringMeetingInvitationEvent extends ApplicationEvent {
    private final Long meetingId;
    private final Long invitedUserId;
    private final String meetingTitle;
    private final String hostName;

    public SpringMeetingInvitationEvent(Object source, Long meetingId, Long invitedUserId, String meetingTitle, String hostName) {
        super(source);
        this.meetingId = meetingId;
        this.invitedUserId = invitedUserId;
        this.meetingTitle = meetingTitle;
        this.hostName = hostName;
    }

    public Long getMeetingId() { return meetingId; }
    public Long getInvitedUserId() { return invitedUserId; }
    public String getMeetingTitle() { return meetingTitle; }
    public String getHostName() { return hostName; }
}
