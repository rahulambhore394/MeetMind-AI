package com.meetmind.meetmind_backend.meeting.dto;

import java.time.Instant;

public class MediaTokenResponse {
    private String meetingId;
    private String rtcUrl;
    private String token;
    private String participantId;
    private MediaPermissionsDto permissions;
    private Instant expiresAt;

    public MediaTokenResponse() {
    }

    public MediaTokenResponse(String meetingId, String rtcUrl, String token, String participantId, MediaPermissionsDto permissions, Instant expiresAt) {
        this.meetingId = meetingId;
        this.rtcUrl = rtcUrl;
        this.token = token;
        this.participantId = participantId;
        this.permissions = permissions;
        this.expiresAt = expiresAt;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getRtcUrl() {
        return rtcUrl;
    }

    public void setRtcUrl(String rtcUrl) {
        this.rtcUrl = rtcUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public MediaPermissionsDto getPermissions() {
        return permissions;
    }

    public void setPermissions(MediaPermissionsDto permissions) {
        this.permissions = permissions;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

