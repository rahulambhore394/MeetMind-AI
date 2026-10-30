package com.meetmind.meetmind_backend.intelligence;

public record ParkingQuestion(
        Long id,
        String question,
        String askedBy,
        Long timestampMs,
        String timeFormatted,
        String status, // UNANSWERED, PARKING_LOT, RESOLVED
        String notes
) {
    public static String formatMs(long ms) {
        long totalSecs = Math.max(0, ms / 1000);
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
