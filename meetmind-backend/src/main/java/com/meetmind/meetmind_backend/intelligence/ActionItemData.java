package com.meetmind.meetmind_backend.intelligence;

public record ActionItemData(
        String description,
        String assignedUser,
        String dueDate,
        Double confidence,
        String verificationStatus
) {}
