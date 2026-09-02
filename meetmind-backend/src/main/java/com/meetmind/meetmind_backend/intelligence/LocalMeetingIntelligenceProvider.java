package com.meetmind.meetmind_backend.intelligence;

import com.meetmind.meetmind_backend.transcription.TranscriptSegment;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LocalMeetingIntelligenceProvider implements MeetingIntelligenceProvider {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "by", "about",
            "against", "between", "into", "through", "during", "before", "after", "above", "below",
            "from", "up", "down", "out", "off", "over", "under", "again", "further", "then",
            "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so",
            "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did",
            "doing", "i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours",
            "he", "him", "his", "she", "her", "hers", "it", "its", "they", "them", "their", "this", "that"
    ));

    private static final List<String> DECISION_MARKERS = List.of(
            "agreed", "decided", "will use", "approved", "chosen", "resolved", "finalized", "settled on", "conclusion is"
    );

    private static final List<String> ACTION_MARKERS = List.of(
            "will do", "will prepare", "will complete", "will update", "will create", "will fix", "will review", "will handle", "will lead",
            "assigned to", "todo", "action item", "need to", "please ensure", "takes responsibility", "by friday", "by monday", "by tuesday", "by wednesday", "by thursday", "by tomorrow", "by next week", "by eod", "must complete", "task is"
    );

    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
            "(?i)\\b(by|due|before)\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|tomorrow|next week|end of day|eod|today|[0-9]{1,2}\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))\\b"
    );

    private static final Pattern ASSIGNEE_PATTERN = Pattern.compile(
            "(?i)\\b(assigned to|assign to|owner is|lead is|@|responsibility of)\\s+([A-Z][a-z]+|[a-z]+)\\b"
    );

    @Override
    public String providerName() {
        return "local-heuristic";
    }

    @Override
    public boolean supportsLanguage(String language) {
        if (language == null) return true;
        String lang = language.toLowerCase();
        return lang.startsWith("en") || lang.startsWith("hi") || lang.startsWith("mr");
    }

    @Override
    public IntelligenceResult analyze(String fullText, List<TranscriptSegment> segments, String language) throws IntelligenceException {
        if (fullText != null && fullText.contains("FAIL_PROVIDER")) {
            throw new IntelligenceException("Simulated AI provider failure for testing");
        }

        if (fullText == null || fullText.trim().isEmpty()) {
            return generateEmptyResult();
        }

        String cleanedText = fullText.trim();
        List<String> sentences = splitSentences(cleanedText);

        // 1. Summary
        String summary = generateSummary(sentences);

        // 2. Key Points
        List<String> keyPoints = extractKeyPoints(sentences);

        // 3. Decisions
        List<String> decisions = extractDecisions(sentences);

        // 4. Action Items
        List<ActionItemData> actionItems = extractActionItems(sentences);

        // 5. Important Topics
        List<String> topics = extractTopics(cleanedText);

        // 6. Questions
        List<String> questions = extractQuestions(sentences);

        // 7. Analysis Metrics
        Map<String, Object> metrics = generateAnalysisMetrics(cleanedText, sentences, segments);

        return new IntelligenceResult(
                summary,
                keyPoints,
                decisions,
                actionItems,
                topics,
                questions,
                metrics,
                providerName(),
                "Local Heuristic Engine v1.0"
        );
    }

    private IntelligenceResult generateEmptyResult() {
        return new IntelligenceResult(
                "No transcript content provided for analysis.",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(
                        "wordCount", 0,
                        "sentenceCount", 0,
                        "speakerCount", 0,
                        "status", "EMPTY_TRANSCRIPT"
                ),
                providerName(),
                "Local Heuristic Engine v1.0"
        );
    }

    private List<String> splitSentences(String text) {
        String[] raw = text.split("(?<=[.!?])\\s+");
        List<String> list = new ArrayList<>();
        for (String r : raw) {
            String trimmed = r.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        if (list.isEmpty() && !text.isBlank()) {
            list.add(text);
        }
        return list;
    }

    private String generateSummary(List<String> sentences) {
        if (sentences.isEmpty()) {
            return "No content available.";
        }
        if (sentences.size() <= 3) {
            return String.join(" ", sentences);
        }
        // Pick first sentence, a middle sentence with key info, and last sentence
        StringBuilder sb = new StringBuilder();
        sb.append(sentences.get(0));
        if (sentences.size() > 3) {
            sb.append(" ").append(sentences.get(sentences.size() / 2));
        }
        sb.append(" ").append(sentences.get(sentences.size() - 1));
        return sb.toString();
    }

    private List<String> extractKeyPoints(List<String> sentences) {
        List<String> points = new ArrayList<>();
        for (String sentence : sentences) {
            // Include sentences with numbers, percentages, or strong statements
            if (sentence.matches(".*\\b(\\d+|percent|%|million|billion|quarter|report|target|goal|launch|release|deadline)\\b.*(?i)")) {
                points.add(sentence);
            }
        }
        if (points.isEmpty() && !sentences.isEmpty()) {
            points.add(sentences.get(0));
        }
        return points.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private List<String> extractDecisions(List<String> sentences) {
        List<String> decisions = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            for (String marker : DECISION_MARKERS) {
                if (lower.contains(marker)) {
                    decisions.add(sentence);
                    break;
                }
            }
        }
        return decisions;
    }

    private List<ActionItemData> extractActionItems(List<String> sentences) {
        List<ActionItemData> items = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            boolean isAction = false;
            for (String marker : ACTION_MARKERS) {
                if (lower.contains(marker)) {
                    isAction = true;
                    break;
                }
            }
            if (!isAction) {
                if (DUE_DATE_PATTERN.matcher(sentence).find() || ASSIGNEE_PATTERN.matcher(sentence).find()) {
                    isAction = true;
                }
            }
            if (isAction) {
                String assignee = extractAssignee(sentence);
                String dueDate = extractDueDate(sentence);

                double confidence = 0.60;
                if (assignee != null) confidence += 0.20;
                if (dueDate != null) confidence += 0.15;
                confidence = Math.min(1.0, confidence);

                String status = (confidence >= 0.8) ? "HIGH_CONFIDENCE" : "NEEDS_REVIEW";

                items.add(new ActionItemData(
                        sentence,
                        assignee,
                        dueDate,
                        confidence,
                        status
                ));
            }
        }
        return items;
    }

    private String extractAssignee(String sentence) {
        Matcher m = ASSIGNEE_PATTERN.matcher(sentence);
        if (m.find()) {
            return m.group(2);
        }
        // Fallback simple search: "X will..."
        Pattern p2 = Pattern.compile("(?i)\\b([A-Z][a-z]+)\\s+will\\b");
        Matcher m2 = p2.matcher(sentence);
        if (m2.find()) {
            String name = m2.group(1);
            if (!STOP_WORDS.contains(name.toLowerCase())) {
                return name;
            }
        }
        return null;
    }

    private String extractDueDate(String sentence) {
        Matcher m = DUE_DATE_PATTERN.matcher(sentence);
        if (m.find()) {
            return m.group(0);
        }
        return null;
    }

    private List<String> extractTopics(String text) {
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().split("\\s+");
        Map<String, Integer> freqMap = new HashMap<>();
        for (String w : words) {
            if (w.length() > 3 && !STOP_WORDS.contains(w)) {
                freqMap.put(w, freqMap.getOrDefault(w, 0) + 1);
            }
        }
        return freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> extractQuestions(List<String> sentences) {
        List<String> questions = new ArrayList<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.endsWith("?") || trimmed.toLowerCase().matches("(?i)^(what|why|how|when|who|where|can we|should we|is there|do we).*")) {
                questions.add(trimmed);
            }
        }
        return questions;
    }

    private Map<String, Object> generateAnalysisMetrics(String fullText, List<String> sentences, List<TranscriptSegment> segments) {
        Map<String, Object> metrics = new HashMap<>();
        int wordCount = fullText.split("\\s+").length;
        metrics.put("wordCount", wordCount);
        metrics.put("sentenceCount", sentences.size());

        if (segments != null && !segments.isEmpty()) {
            metrics.put("segmentCount", segments.size());
            Set<String> speakers = new HashSet<>();
            Map<String, Integer> speakerWords = new HashMap<>();
            for (TranscriptSegment seg : segments) {
                String speaker = seg.getSpeaker() != null ? seg.getSpeaker() : "Unknown";
                speakers.add(speaker);
                int segWords = seg.getText() != null ? seg.getText().split("\\s+").length : 0;
                speakerWords.put(speaker, speakerWords.getOrDefault(speaker, 0) + segWords);
            }
            metrics.put("speakerCount", speakers.size());
            metrics.put("speakerWordDistribution", speakerWords);
        } else {
            metrics.put("speakerCount", 1);
        }

        // Basic sentiment estimation
        String lower = fullText.toLowerCase();
        int positiveCount = countOccurrences(lower, List.of("great", "good", "excellent", "agree", "progress", "success", "approved"));
        int negativeCount = countOccurrences(lower, List.of("issue", "problem", "delay", "risk", "concern", "fail", "reject"));

        String sentiment = "NEUTRAL";
        if (positiveCount > negativeCount) sentiment = "POSITIVE";
        else if (negativeCount > positiveCount) sentiment = "CONCERNED";

        metrics.put("sentiment", sentiment);
        return metrics;
    }

    private int countOccurrences(String text, List<String> keywords) {
        int count = 0;
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = text.indexOf(kw, idx)) != -1) {
                count++;
                idx += kw.length();
            }
        }
        return count;
    }
}
