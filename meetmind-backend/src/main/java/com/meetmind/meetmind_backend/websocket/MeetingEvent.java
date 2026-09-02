package com.meetmind.meetmind_backend.websocket;



public class MeetingEvent {

    private MeetingEventType type;

    private Long meetingId;

    private Long userId;

    private String userName;

    private String message;


    public MeetingEvent() {
    }


    public MeetingEvent(
            MeetingEventType type,
            Long meetingId,
            Long userId,
            String userName,
            String message
    ) {

        this.type = type;
        this.meetingId = meetingId;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
    }


    public MeetingEventType getType() {
        return type;
    }


    public void setType(MeetingEventType type) {
        this.type = type;
    }


    public Long getMeetingId() {
        return meetingId;
    }


    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }


    public Long getUserId() {
        return userId;
    }


    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public String getUserName() {
        return userName;
    }


    public void setUserName(String userName) {
        this.userName = userName;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }
}