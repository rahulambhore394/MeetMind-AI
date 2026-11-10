package com.meetmind.meetmind_backend.meeting.dto;

public class MediaPermissionsDto {
    private boolean canPublishAudio;
    private boolean canPublishVideo;
    private boolean canPublishScreen;
    private boolean canSubscribe;

    public MediaPermissionsDto() {
    }

    public MediaPermissionsDto(boolean canPublishAudio, boolean canPublishVideo, boolean canPublishScreen, boolean canSubscribe) {
        this.canPublishAudio = canPublishAudio;
        this.canPublishVideo = canPublishVideo;
        this.canPublishScreen = canPublishScreen;
        this.canSubscribe = canSubscribe;
    }

    public boolean isCanPublishAudio() {
        return canPublishAudio;
    }

    public void setCanPublishAudio(boolean canPublishAudio) {
        this.canPublishAudio = canPublishAudio;
    }

    public boolean isCanPublishVideo() {
        return canPublishVideo;
    }

    public void setCanPublishVideo(boolean canPublishVideo) {
        this.canPublishVideo = canPublishVideo;
    }

    public boolean isCanPublishScreen() {
        return canPublishScreen;
    }

    public void setCanPublishScreen(boolean canPublishScreen) {
        this.canPublishScreen = canPublishScreen;
    }

    public boolean isCanSubscribe() {
        return canSubscribe;
    }

    public void setCanSubscribe(boolean canSubscribe) {
        this.canSubscribe = canSubscribe;
    }
}

