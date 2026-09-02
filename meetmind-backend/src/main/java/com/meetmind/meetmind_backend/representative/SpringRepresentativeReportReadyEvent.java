package com.meetmind.meetmind_backend.representative;

import org.springframework.context.ApplicationEvent;

public class SpringRepresentativeReportReadyEvent extends ApplicationEvent {

    private final Long reportId;
    private final Long representativeId;
    private final Long meetingId;
    private final Long ownerId;

    public SpringRepresentativeReportReadyEvent(Object source, Long reportId, Long representativeId, Long meetingId, Long ownerId) {
        super(source);
        this.reportId = reportId;
        this.representativeId = representativeId;
        this.meetingId = meetingId;
        this.ownerId = ownerId;
    }

    public Long getReportId() { return reportId; }
    public Long getRepresentativeId() { return representativeId; }
    public Long getMeetingId() { return meetingId; }
    public Long getOwnerId() { return ownerId; }
}
