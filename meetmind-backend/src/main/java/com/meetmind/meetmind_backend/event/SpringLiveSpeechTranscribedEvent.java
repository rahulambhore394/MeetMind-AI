package com.meetmind.meetmind_backend.event;

import org.springframework.context.ApplicationEvent;

public class SpringLiveSpeechTranscribedEvent extends ApplicationEvent {
    private final Long meetingId;
    private final Long userId;
    private final String speaker;
    private final String speechText;

    public SpringLiveSpeechTranscribedEvent(
            Object source,
            Long meetingId,
            Long userId,
            String speaker,
            String speechText
    ) {
        super(source);
        this.meetingId = meetingId;
        this.userId = userId;
        this.speaker = speaker;
        this.speechText = speechText;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getSpeechText() {
        return speechText;
    }
}

