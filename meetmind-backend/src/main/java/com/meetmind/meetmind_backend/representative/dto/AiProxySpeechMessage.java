package com.meetmind.meetmind_backend.representative.dto;

public record AiProxySpeechMessage(
        Long meetingId,
        Long representativeId,
        Long ownerId,
        String ownerName,
        String query,
        String spokenText,
        String language,
        long timestamp,
        String avatarUrl
) {
    public AiProxySpeechMessage(
            Long meetingId,
            Long representativeId,
            Long ownerId,
            String ownerName,
            String query,
            String spokenText,
            String language,
            long timestamp
    ) {
        this(meetingId, representativeId, ownerId, ownerName, query, spokenText, language, timestamp, null);
    }
}
