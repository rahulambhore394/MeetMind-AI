package com.meetmind.meetmind_backend.intelligence.dto;

import java.time.LocalDateTime;

public class MeetingReportSummaryDto {
    private Long meetingId;
    private String title;
    private String description;
    private String meetingCode;
    private String hostName;
    private String startedAt;
    private String endedAt;
    private String meetingType; // "NORMAL" or "AI_REPRESENTATIVE"
    private String status;
    private String summarySnippet;
    private int actionItemCount;
    private int myTaskCount;
    private boolean hasAiRepresentative;

    public MeetingReportSummaryDto() {}

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMeetingCode() { return meetingCode; }
    public void setMeetingCode(String meetingCode) { this.meetingCode = meetingCode; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSummarySnippet() { return summarySnippet; }
    public void setSummarySnippet(String summarySnippet) { this.summarySnippet = summarySnippet; }

    public int getActionItemCount() { return actionItemCount; }
    public void setActionItemCount(int actionItemCount) { this.actionItemCount = actionItemCount; }

    public int getMyTaskCount() { return myTaskCount; }
    public void setMyTaskCount(int myTaskCount) { this.myTaskCount = myTaskCount; }

    public boolean isHasAiRepresentative() { return hasAiRepresentative; }
    public void setHasAiRepresentative(boolean hasAiRepresentative) { this.hasAiRepresentative = hasAiRepresentative; }
}
