package com.meetmind.meetmind_backend.intelligence;

import org.springframework.context.ApplicationEvent;

public class SpringActionItemAssignedEvent extends ApplicationEvent {
    private final Long actionItemId;
    private final Long meetingId;
    private final String assignedUser;
    private final String description;

    public SpringActionItemAssignedEvent(Object source, Long actionItemId, Long meetingId, String assignedUser, String description) {
        super(source);
        this.actionItemId = actionItemId;
        this.meetingId = meetingId;
        this.assignedUser = assignedUser;
        this.description = description;
    }

    public Long getActionItemId() { return actionItemId; }
    public Long getMeetingId() { return meetingId; }
    public String getAssignedUser() { return assignedUser; }
    public String getDescription() { return description; }
}
