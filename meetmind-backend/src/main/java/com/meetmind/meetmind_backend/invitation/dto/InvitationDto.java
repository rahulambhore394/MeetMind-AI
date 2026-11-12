package com.meetmind.meetmind_backend.invitation.dto;

import com.meetmind.meetmind_backend.meeting.Meeting;
import com.meetmind.meetmind_backend.meeting.MeetingStatus;
import com.meetmind.meetmind_backend.participant.MeetingParticipant;
import com.meetmind.meetmind_backend.user.User;

import java.time.LocalDateTime;

public class InvitationDto {
    private Long id;
    private Long meetingId;
    private String meetingCode;
    private String meetingTitle;
    private String meetingDescription;
    private Long hostId;
    private String hostName;
    private String hostEmail;
    private String status;
    private LocalDateTime scheduledAt;
    private boolean isLive;

    public InvitationDto() {}

    public InvitationDto(MeetingParticipant participant) {
        if (participant != null) {
            this.id = participant.getId();
            this.status = participant.getStatus() != null ? participant.getStatus().name() : "INVITED";
            
            Meeting meeting = participant.getMeeting();
            if (meeting != null) {
                this.meetingId = meeting.getId();
                this.meetingCode = meeting.getMeetingCode();
                this.meetingTitle = meeting.getTitle();
                this.meetingDescription = meeting.getDescription();
                this.scheduledAt = meeting.getScheduledAt();
                this.isLive = meeting.getStatus() == MeetingStatus.LIVE;

                User host = meeting.getHost();
                if (host != null) {
                    this.hostId = host.getId();
                    this.hostName = host.getName();
                    this.hostEmail = host.getEmail();
                }
            }
        }
    }

    public InvitationDto(Meeting meeting, String participantStatus) {
        if (meeting != null) {
            this.meetingId = meeting.getId();
            this.meetingCode = meeting.getMeetingCode();
            this.meetingTitle = meeting.getTitle();
            this.meetingDescription = meeting.getDescription();
            this.scheduledAt = meeting.getScheduledAt();
            this.status = participantStatus != null ? participantStatus : "INVITED";
            this.isLive = meeting.getStatus() == MeetingStatus.LIVE;

            User host = meeting.getHost();
            if (host != null) {
                this.hostId = host.getId();
                this.hostName = host.getName();
                this.hostEmail = host.getEmail();
            }
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getMeetingCode() { return meetingCode; }
    public void setMeetingCode(String meetingCode) { this.meetingCode = meetingCode; }

    public String getMeetingTitle() { return meetingTitle; }
    public void setMeetingTitle(String meetingTitle) { this.meetingTitle = meetingTitle; }

    public String getMeetingDescription() { return meetingDescription; }
    public void setMeetingDescription(String meetingDescription) { this.meetingDescription = meetingDescription; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostEmail() { return hostEmail; }
    public void setHostEmail(String hostEmail) { this.hostEmail = hostEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }
}

