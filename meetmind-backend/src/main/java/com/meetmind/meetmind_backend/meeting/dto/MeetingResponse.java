package com.meetmind.meetmind_backend.meeting.dto;


import com.meetmind.meetmind_backend.meeting.Meeting;

import java.time.LocalDateTime;

public class MeetingResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String status;
    private Long id;
    private String title;
    private String description;

    private Long hostId;
    private String hostName;
    private String hostEmail;

    private String meetingCode;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private Boolean hasJoinedBefore = false;
    private String userParticipantStatus;

    public MeetingResponse(Meeting meeting) {
        this(meeting, false, null);
    }

    public MeetingResponse(Meeting meeting, Boolean hasJoinedBefore, String userParticipantStatus) {
        this.id = meeting.getId();
        this.title = meeting.getTitle();
        this.description = meeting.getDescription();
        this.meetingCode = meeting.getMeetingCode();

        this.hostId = meeting.getHost().getId();
        this.hostName = meeting.getHost().getName();
        this.hostEmail = meeting.getHost().getEmail();

        this.scheduledAt = meeting.getScheduledAt();
        this.startedAt = meeting.getStartedAt();
        this.endedAt = meeting.getEndedAt();
        this.createdAt = meeting.getCreatedAt();
        this.status = meeting.getStatus().name();
        this.hasJoinedBefore = hasJoinedBefore != null ? hasJoinedBefore : false;
        this.userParticipantStatus = userParticipantStatus;
    }

    public Boolean getHasJoinedBefore() {
        return hasJoinedBefore;
    }

    public String getUserParticipantStatus() {
        return userParticipantStatus;
    }

    public String getMeetingCode() {
        return meetingCode;
    }

    public Long getId() {
        return id;
    }
    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostEmail() {
        return hostEmail;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}