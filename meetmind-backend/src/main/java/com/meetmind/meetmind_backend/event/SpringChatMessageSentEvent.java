package com.meetmind.meetmind_backend.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class SpringChatMessageSentEvent extends ApplicationEvent {
    private final Long meetingId;
    private final Long userId;
    private final String senderName;
    private final Long messageId;
    private final String message;
    private final LocalDateTime sentAt;

    public SpringChatMessageSentEvent(
            Object source,
            Long meetingId,
            Long userId,
            String senderName,
            Long messageId,
            String message,
            LocalDateTime sentAt
    ) {
        super(source);
        this.meetingId = meetingId;
        this.userId = userId;
        this.senderName = senderName;
        this.messageId = messageId;
        this.message = message;
        this.sentAt = sentAt;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSenderName() {
        return senderName;
    }

    public Long getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
