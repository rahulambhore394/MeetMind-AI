package com.meetmind.meetmind_backend.meeting.dto;


import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateMeetingRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @NotNull(message = "Scheduled time is required")
    @FutureOrPresent(message = "Meeting scheduled time cannot be in the past")
    private LocalDateTime scheduledAt;

    private java.util.List<String> invitedEmails;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public java.util.List<String> getInvitedEmails() {
        return invitedEmails;
    }

    public void setInvitedEmails(java.util.List<String> invitedEmails) {
        this.invitedEmails = invitedEmails;
    }
}
