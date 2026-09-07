package com.meetmind.meetmind_backend.participant.dto;



import com.meetmind.meetmind_backend.participant.MeetingParticipant;

import java.time.LocalDateTime;

public class ParticipantResponse {

    private Long id;

    private Long userId;

    private String name;

    private String email;

    private String role;

    private String status;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;


    public ParticipantResponse(
            MeetingParticipant participant
    ) {

        this.id = participant.getId();

        this.userId =
                participant.getUser().getId();

        this.name =
                participant.getUser().getName();

        this.email =
                participant.getUser().getEmail();

        this.role =
                participant.getRole().name();

        this.status =
                participant.getStatus().name();

        this.joinedAt =
                participant.getJoinedAt();

        this.leftAt =
                participant.getLeftAt();
    }


    public ParticipantResponse(
            String email,
            String role,
            String status
    ) {
        this.id = null;
        this.userId = null;
        this.name = email;
        this.email = email;
        this.role = role;
        this.status = status;
        this.joinedAt = null;
        this.leftAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }
}
