package com.meetmind.meetmind_backend.intelligence;

import java.util.List;
import java.util.Map;

public record IntelligenceResult(
        String summary,
        List<String> keyPoints,
        List<String> decisions,
        List<ActionItemData> actionItems,
        List<String> topics,
        List<String> questions,
        Map<String, Object> analysisMetrics,
        String providerName,
        String modelMetadata
) {}
