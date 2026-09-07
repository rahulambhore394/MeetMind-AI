package com.meetmind.meetmind_backend.intelligence.dto;

import com.meetmind.meetmind_backend.intelligence.ActionItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ComprehensiveMeetingReportDto {
    private Long meetingId;
    private String title;
    private String description;
    private String meetingCode;
    private String hostName;
    private String startedAt;
    private String endedAt;
    private String meetingType; // "NORMAL" or "AI_REPRESENTATIVE"
    private String status;

    private String executiveSummary;
    private List<String> keyPoints;
    private List<String> decisions;
    private List<String> topics;
    private List<String> questions;
    
    // Work Assignments / Action Items
    private List<ActionItem> allWorkAssignments;
    private List<ActionItem> myAssignedTasks;
    private Map<String, List<ActionItem>> userTaskBreakdown; // User Name -> List of Action Items

    // AI Representative Report Details (if applicable)
    private Long representativeId;
    private List<String> ownerRelevantQuestions;
    private List<String> monitoredTopicsFound;
    private Map<String, Object> attendanceTimeline;

    public ComprehensiveMeetingReportDto() {}

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

    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }

    public List<String> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints; }

    public List<String> getDecisions() { return decisions; }
    public void setDecisions(List<String> decisions) { this.decisions = decisions; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }

    public List<String> getQuestions() { return questions; }
    public void setQuestions(List<String> questions) { this.questions = questions; }

    public List<ActionItem> getAllWorkAssignments() { return allWorkAssignments; }
    public void setAllWorkAssignments(List<ActionItem> allWorkAssignments) { this.allWorkAssignments = allWorkAssignments; }

    public List<ActionItem> getMyAssignedTasks() { return myAssignedTasks; }
    public void setMyAssignedTasks(List<ActionItem> myAssignedTasks) { this.myAssignedTasks = myAssignedTasks; }

    public Map<String, List<ActionItem>> getUserTaskBreakdown() { return userTaskBreakdown; }
    public void setUserTaskBreakdown(Map<String, List<ActionItem>> userTaskBreakdown) { this.userTaskBreakdown = userTaskBreakdown; }

    public Long getRepresentativeId() { return representativeId; }
    public void setRepresentativeId(Long representativeId) { this.representativeId = representativeId; }

    public List<String> getOwnerRelevantQuestions() { return ownerRelevantQuestions; }
    public void setOwnerRelevantQuestions(List<String> ownerRelevantQuestions) { this.ownerRelevantQuestions = ownerRelevantQuestions; }

    public List<String> getMonitoredTopicsFound() { return monitoredTopicsFound; }
    public void setMonitoredTopicsFound(List<String> monitoredTopicsFound) { this.monitoredTopicsFound = monitoredTopicsFound; }

    public Map<String, Object> getAttendanceTimeline() { return attendanceTimeline; }
    public void setAttendanceTimeline(Map<String, Object> attendanceTimeline) { this.attendanceTimeline = attendanceTimeline; }
}
