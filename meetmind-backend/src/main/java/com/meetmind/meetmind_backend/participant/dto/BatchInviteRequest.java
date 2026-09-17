package com.meetmind.meetmind_backend.participant.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchInviteRequest {

    @NotEmpty(message = "At least one email is required")
    private List<String> emails;

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }
}
