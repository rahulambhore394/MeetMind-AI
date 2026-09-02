package com.meetmind.meetmind_backend.representative;

import org.springframework.context.ApplicationEvent;

public class SpringRepresentativeActivatedEvent extends ApplicationEvent {

    private final Long representativeId;
    private final Long meetingId;
    private final Long ownerId;

    public SpringRepresentativeActivatedEvent(Object source, Long representativeId, Long meetingId, Long ownerId) {
        super(source);
        this.representativeId = representativeId;
        this.meetingId = meetingId;
        this.ownerId = ownerId;
    }

    public Long getRepresentativeId() { return representativeId; }
    public Long getMeetingId() { return meetingId; }
    public Long getOwnerId() { return ownerId; }
}
