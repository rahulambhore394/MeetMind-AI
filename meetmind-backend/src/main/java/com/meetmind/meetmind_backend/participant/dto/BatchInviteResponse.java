package com.meetmind.meetmind_backend.participant.dto;

import java.util.List;

public class BatchInviteResponse {

    private int totalSubmitted;
    private int successCount;
    private List<String> invitedEmails;
    private String message;

    public BatchInviteResponse() {}

    public BatchInviteResponse(int totalSubmitted, int successCount, List<String> invitedEmails, String message) {
        this.totalSubmitted = totalSubmitted;
        this.successCount = successCount;
        this.invitedEmails = invitedEmails;
        this.message = message;
    }

    public int getTotalSubmitted() {
        return totalSubmitted;
    }

    public void setTotalSubmitted(int totalSubmitted) {
        this.totalSubmitted = totalSubmitted;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public List<String> getInvitedEmails() {
        return invitedEmails;
    }

    public void setInvitedEmails(List<String> invitedEmails) {
        this.invitedEmails = invitedEmails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
