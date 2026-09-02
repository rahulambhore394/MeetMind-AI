package com.meetmind.meetmind_backend.intelligence;

import org.springframework.context.ApplicationEvent;

public class SpringAiSummaryReadyEvent extends ApplicationEvent {

    private final Long meetingSummaryId;
    private final Long meetingId;
    private final Long transcriptId;
    private final int actionItemCount;
    private final boolean success;

    public SpringAiSummaryReadyEvent(Object source,
                                     Long meetingSummaryId,
                                     Long meetingId,
                                     Long transcriptId,
                                     int actionItemCount,
                                     boolean success) {
        super(source);
        this.meetingSummaryId = meetingSummaryId;
        this.meetingId = meetingId;
        this.transcriptId = transcriptId;
        this.actionItemCount = actionItemCount;
        this.success = success;
    }

    public Long getMeetingSummaryId() { return meetingSummaryId; }
    public Long getMeetingId() { return meetingId; }
    public Long getTranscriptId() { return transcriptId; }
    public int getActionItemCount() { return actionItemCount; }
    public boolean isSuccess() { return success; }
}
