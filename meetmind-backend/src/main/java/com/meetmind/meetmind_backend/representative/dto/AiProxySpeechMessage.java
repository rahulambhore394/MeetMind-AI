package com.meetmind.meetmind_backend.representative.dto;

public record AiProxySpeechMessage(
        Long meetingId,
        Long representativeId,
        Long ownerId,
        String ownerName,
        String query,
        String spokenText,
        String language,
        long timestamp
) {}
