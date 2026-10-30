package com.meetmind.meetmind_backend.intelligence;

import java.util.List;

public record TopicChapter(
        String title,
        Long startMs,
        Long endMs,
        String timeRange,
        String summary,
        List<String> keywords
) {
    public static String formatMs(long ms) {
        long totalSecs = Math.max(0, ms / 1000);
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
