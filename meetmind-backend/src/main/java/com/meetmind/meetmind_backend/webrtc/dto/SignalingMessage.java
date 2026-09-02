package com.meetmind.meetmind_backend.webrtc.dto;

public class SignalingMessage {
    private Long meetingId;
    private Long senderId;
    private String senderName;
    private Long receiverId; // null for broadcast events
    private String type; // "JOIN", "LEAVE", "OFFER", "ANSWER", "ICE_CANDIDATE", "PEER_LIST"
    private String payload; // SDP payload or ICE candidate details

    public SignalingMessage() {}

    public SignalingMessage(Long meetingId, Long senderId, String senderName, Long receiverId, String type, String payload) {
        this.meetingId = meetingId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.type = type;
        this.payload = payload;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
