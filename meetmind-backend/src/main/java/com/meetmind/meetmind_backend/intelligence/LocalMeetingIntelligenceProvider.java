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
            "we settled on", "settled on", "we approve", "we approved", "approved", "is approved",
            "let's go with option", "lets go with option", "let's go with", "lets go with", "let us go with",
            "agreed on", "agreed upon", "we agreed", "agreement reached", "consensus reached", "consensus is",
            "we decided to", "we decided on", "decided to", "decided on", "decision made", "decision is",
            "selected", "we selected", "chosen", "chose", "resolution is", "resolved to", "resolved that",
            "finalized", "unanimously agreed", "all in favor", "signed off on", "conclusion is", "will use", "moving forward with"
    );

    private static final List<String> ACTION_MARKERS = List.of(
            "i will prepare", "will prepare", "i will do", "will do", "i will complete", "will complete",
            "i will update", "will update", "i will create", "will create", "i will fix", "will fix",
            "i will review", "will review", "i will handle", "will handle", "i will lead", "will lead",
            "i will send", "will send", "i will deploy", "will deploy", "i will schedule", "will schedule",
            "assigned to", "assign to", "assigning to", "owner is", "ownership of", "responsibility of", "responsible for",
            "due next monday", "due next week", "due tomorrow", "due on", "due by",
            "by friday", "by monday", "by tuesday", "by wednesday", "by thursday", "by tomorrow", "by next week", "by eod", "by end of day",
            "action item", "action items", "todo", "need to", "please ensure", "takes responsibility", "must complete", "task is", "task:"
    );

    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
            "(?i)\\b(by|due|before|deadline is|due on|due by)\\s+(the\\s+)?(next\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|tomorrow|next week|end of day|eod|today|this week|end of week|[0-9]{1,2}(?:st|nd|rd|th)?\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|[a-z]+)?|[0-9]{1,2}:[0-9]{2}\\s*(?:am|pm)?)\\b"
    );

    private static final Pattern ASSIGNEE_PATTERN = Pattern.compile(
            "(?i)\\b(assigned to|assign to|assigning to|owner is|lead is|@|responsibility of|responsible for)\\s+([A-Z][a-z]+|[a-z]+)\\b"
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
        List<ActionItemData> actionItems = extractActionItems(sentences, segments);

        // 5. Topic Chapterization & Smart Timestamps
        List<TopicChapter> chapters = extractChapters(cleanedText, sentences, segments);
        List<String> topics = chapters.stream()
                .map(c -> c.timeRange() + ": " + c.title())
                .collect(Collectors.toList());
        if (topics.isEmpty()) {
            topics = extractTopics(cleanedText);
        }

        // 6. Unanswered Questions & Parking Lot Items
        List<ParkingQuestion> parkingLotItems = extractParkingQuestions(sentences, segments);
        List<String> questions = parkingLotItems.stream()
                .map(q -> "[" + q.status() + "] " + q.question())
                .collect(Collectors.toList());
        if (questions.isEmpty()) {
            questions = extractQuestions(sentences);
        }

        // 7. Analysis Metrics
        Map<String, Object> metrics = generateAnalysisMetrics(cleanedText, sentences, segments);
        metrics.put("chapters", chapters);
        metrics.put("parkingLotItems", parkingLotItems);

        return new IntelligenceResult(
                summary,
                keyPoints,
                decisions,
                actionItems,
                topics,
                questions,
                metrics,
                providerName(),
                "Local Heuristic Engine v1.0",
                chapters,
                parkingLotItems
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

    public List<String> splitSentences(String text) {
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

        // Filter out conversational pleasantries and audio-check fillers
        List<String> meaningful = sentences.stream()
                .map(String::trim)
                .filter(s -> !isFillerSentence(s))
                .collect(Collectors.toList());

        if (meaningful.isEmpty()) {
            meaningful = sentences;
        }

        // If the meaningful sentences are already 3 to 5, format and return
        if (meaningful.size() >= 3 && meaningful.size() <= 5) {
            return formatBrief(meaningful);
        }

        if (meaningful.size() < 3) {
            // If less than 3 sentences, expand or retain what is present
            return formatBrief(meaningful);
        }

        // For longer meetings (e.g. 30-60 min discussions), select 3 to 5 key executive sentences:
        // Pillar 1: Context & Objective (agenda, review, budget, roadmap, architecture)
        // Pillar 2: Core Challenge / Metric / Driver (percentage, compute, increase, cost, issue)
        // Pillar 3: Definitive Decision (decided, agreed, migrate, approved, will use)
        // Pillar 4: Timeline / Rollout / Next Steps (slated, deployment, friday, deadline, rollout)

        String contextSentence = findBestSentence(meaningful, List.of("reviewed", "review", "budget", "roadmap", "objective", "agenda", "infrastructure", "architecture", "strategy", "project", "discussed"));
        String metricSentence = findBestSentence(meaningful, List.of("percent", "%", "increase", "decrease", "usage", "compute", "cost", "costs", "budget", "exceeding", "due to", "because", "latency", "million", "billion"));
        String decisionSentence = findBestSentence(meaningful, DECISION_MARKERS);
        String timelineSentence = findBestSentence(meaningful, List.of("slated", "deployment", "deploy", "scheduled", "rollout", "friday", "monday", "next week", "timeline", "launch", "release", "milestone"));

        List<String> selected = new ArrayList<>();
        if (contextSentence != null) selected.add(contextSentence);
        if (metricSentence != null && !selected.contains(metricSentence)) selected.add(metricSentence);
        if (decisionSentence != null && !selected.contains(decisionSentence)) selected.add(decisionSentence);
        if (timelineSentence != null && !selected.contains(timelineSentence)) selected.add(timelineSentence);

        // Ensure we have between 3 and 5 sentences
        if (selected.size() < 3) {
            for (String s : meaningful) {
                if (!selected.contains(s)) {
                    selected.add(s);
                    if (selected.size() >= 3) break;
                }
            }
        } else if (selected.size() > 5) {
            selected = selected.subList(0, 5);
        }

        // Sort selected sentences based on their original order in transcript to maintain logical narrative flow
        final List<String> finalMeaningful = meaningful;
        selected.sort(Comparator.comparingInt(finalMeaningful::indexOf));

        return formatBrief(selected);
    }

    private boolean isFillerSentence(String s) {
        String lower = s.toLowerCase();
        return lower.matches("(?i).*(can you hear me|can everyone hear|good morning|hello everyone|thanks for joining|see you later|have a good day|bye bye|bye everyone|screen visible|sound check).*")
                || (s.split("\\s+").length <= 3 && !s.contains("%") && !s.matches(".*\\d+.*"));
    }

    private String findBestSentence(List<String> sentences, List<String> keywords) {
        String best = null;
        int bestScore = 0;
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            int score = 0;
            for (String kw : keywords) {
                if (lower.contains(kw)) {
                    score += 2;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = sentence;
            }
        }
        return best;
    }

    private String formatBrief(List<String> sentences) {
        StringBuilder sb = new StringBuilder();
        for (String s : sentences) {
            String clean = s.trim();
            if (clean.isEmpty()) continue;
            // Capitalize first letter
            clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
            if (!clean.endsWith(".") && !clean.endsWith("!") && !clean.endsWith("?")) {
                clean += ".";
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(clean);
        }
        return sb.toString();
    }

    private List<String> extractKeyPoints(List<String> sentences) {
        List<String> points = new ArrayList<>();
        // 1. Capture major decisions as bullet points
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            for (String marker : DECISION_MARKERS) {
                if (lower.contains(marker)) {
                    points.add(cleanBulletPoint(sentence));
                    break;
                }
            }
        }

        // 2. Capture numerical/budget/metric statements
        for (String sentence : sentences) {
            if (sentence.matches(".*\\b(\\d+|percent|%|million|billion|quarter|budget|cost|usage)\\b.*(?i)")) {
                String bullet = cleanBulletPoint(sentence);
                if (!points.contains(bullet)) {
                    points.add(bullet);
                }
            }
        }

        // 3. Capture rollout/timeline statements
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            if (lower.contains("slated") || lower.contains("deployment") || lower.contains("scheduled") || lower.contains("launch") || lower.contains("deadline")) {
                String bullet = cleanBulletPoint(sentence);
                if (!points.contains(bullet)) {
                    points.add(bullet);
                }
            }
        }

        if (points.isEmpty() && !sentences.isEmpty()) {
            points.add(cleanBulletPoint(sentences.get(0)));
        }

        return points.stream().distinct().limit(6).collect(Collectors.toList());
    }

    private String cleanBulletPoint(String sentence) {
        String clean = sentence.trim();
        clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        if (!clean.endsWith(".") && !clean.endsWith("!") && !clean.endsWith("?")) {
            clean += ".";
        }
        return clean;
    }

    private List<String> extractDecisions(List<String> sentences) {
        List<String> decisions = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            for (String marker : DECISION_MARKERS) {
                if (lower.contains(marker)) {
                    String clean = cleanDecisionSentence(sentence);
                    if (!clean.isBlank() && !decisions.contains(clean)) {
                        decisions.add(clean);
                    }
                    break;
                }
            }
        }
        return decisions;
    }

    private String cleanDecisionSentence(String sentence) {
        if (sentence == null) return "";
        String clean = sentence.trim();
        // Remove conversational prefixes like "Host said: ", "Dave stated: ", "Bob: "
        clean = clean.replaceFirst("^(?i)[A-Za-z0-9_ ]+?\\s*(said|stated|announced)?\\s*:\\s*", "").trim();
        if (clean.isEmpty()) return "";

        clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        if (!clean.endsWith(".") && !clean.endsWith("!") && !clean.endsWith("?")) {
            clean += ".";
        }
        return clean;
    }

    private List<ActionItemData> extractActionItems(List<String> sentences, List<TranscriptSegment> segments) {
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
                String assignee = extractAssignee(sentence, segments);
                String dueDate = extractDueDate(sentence);

                double confidence = 0.65;
                if (assignee != null) confidence += 0.20;
                if (dueDate != null) confidence += 0.15;
                confidence = Math.min(0.98, confidence);

                String status = "PENDING";
                String cleanDescription = cleanActionDescription(sentence, assignee, dueDate);

                items.add(new ActionItemData(
                        cleanDescription,
                        assignee,
                        dueDate,
                        confidence,
                        status
                ));
            }
        }
        return items;
    }

    private String cleanActionDescription(String sentence, String assignee, String dueDate) {
        String clean = sentence.trim();
        // Remove trailing due date
        if (dueDate != null && clean.toLowerCase().endsWith(dueDate.toLowerCase())) {
            clean = clean.substring(0, clean.length() - dueDate.length()).trim();
            clean = clean.replaceAll("(?i)\\s+(by|due|before|on)$", "").trim();
        }
        // Extract verb phrase if prefixed with speaker/assignee/I will
        Pattern p = Pattern.compile("(?i)^(?:[A-Za-z]+\\s+will|i\\s+will|i'll|please|we\\s+need\\s+to|need\\s+to|action\\s+item:?|todo:?)\\s+(.+)");
        Matcher m = p.matcher(clean);
        if (m.find()) {
            clean = m.group(1).trim();
        }
        if (!clean.isEmpty()) {
            clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        }
        if (clean.endsWith(".")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        return clean.isEmpty() ? sentence : clean;
    }

    private String extractAssignee(String sentence, List<TranscriptSegment> segments) {
        Matcher m = ASSIGNEE_PATTERN.matcher(sentence);
        if (m.find()) {
            String name = m.group(2);
            if (!STOP_WORDS.contains(name.toLowerCase())) {
                return capitalize(name);
            }
        }

        // If sentence contains "I will" or "I'll", resolve speaker from transcript segments
        if (sentence.matches("(?i).*\\b(i will|i'll|i am going to)\\b.*") && segments != null) {
            for (TranscriptSegment seg : segments) {
                if (seg.getText() != null && (seg.getText().contains(sentence) || sentence.contains(seg.getText()))) {
                    if (seg.getSpeaker() != null && !seg.getSpeaker().isBlank() && !seg.getSpeaker().equalsIgnoreCase("unknown")) {
                        return capitalize(seg.getSpeaker());
                    }
                }
            }
        }

        // Fallback simple search: "X will..."
        Pattern p2 = Pattern.compile("(?i)\\b([A-Z][a-z]+)\\s+will\\b");
        Matcher m2 = p2.matcher(sentence);
        if (m2.find()) {
            String name = m2.group(1);
            if (!STOP_WORDS.contains(name.toLowerCase())) {
                return capitalize(name);
            }
        }
        return null;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
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

    public List<TopicChapter> extractChapters(String text, List<String> sentences, List<TranscriptSegment> segments) {
        List<TopicChapter> chapters = new ArrayList<>();
        if (segments == null || segments.isEmpty()) {
            if (sentences.isEmpty()) return chapters;
            long startMs = 0L;
            long endMs = Math.max(60000L, (long) sentences.size() * 30000L);
            String title = determineChapterTitle(text, 0);
            String summary = cleanBulletPoint(sentences.get(0));
            List<String> kw = extractTopKeywords(text, 4);
            chapters.add(new TopicChapter(title, startMs, endMs, TopicChapter.formatMs(startMs) + " - " + TopicChapter.formatMs(endMs), summary, kw));
            return chapters;
        }

        int segCount = segments.size();
        int chapterCount = Math.min(Math.max(2, segCount / 2), 5);
        int chunkSize = Math.max(1, (int) Math.ceil((double) segCount / chapterCount));

        for (int i = 0; i < segCount; i += chunkSize) {
            int endIdx = Math.min(i + chunkSize, segCount);
            List<TranscriptSegment> chunk = segments.subList(i, endIdx);
            if (chunk.isEmpty()) continue;

            TranscriptSegment first = chunk.get(0);
            TranscriptSegment last = chunk.get(chunk.size() - 1);
            long startMs = first.getStartMs();
            long endMs = last.getEndMs() > 0 ? last.getEndMs() : (startMs + 60000L);
            if (endMs <= startMs) endMs = startMs + 30000L;

            StringBuilder chunkText = new StringBuilder();
            for (TranscriptSegment s : chunk) {
                if (s.getText() != null) {
                    if (chunkText.length() > 0) chunkText.append(" ");
                    chunkText.append(s.getText());
                }
            }

            String chunkString = chunkText.toString();
            String title = determineChapterTitle(chunkString, chapters.size());
            String chapterSummary = synthesizeChapterSummary(chunk);
            List<String> keywords = extractTopKeywords(chunkString, 4);

            String timeRange = TopicChapter.formatMs(startMs) + " - " + TopicChapter.formatMs(endMs);
            chapters.add(new TopicChapter(title, startMs, endMs, timeRange, chapterSummary, keywords));
        }

        return chapters;
    }

    private String determineChapterTitle(String chunkText, int index) {
        String lower = chunkText.toLowerCase();
        if (lower.contains("budget") || lower.contains("cost") || lower.contains("usage") || lower.contains("introduction") || lower.contains("intro")) {
            return "Introduction & Budget Review";
        }
        if (lower.contains("architecture") || lower.contains("database") || lower.contains("postgresql") || lower.contains("mongodb") || lower.contains("technical")) {
            return "Technical Architecture Discussion";
        }
        if (lower.contains("release") || lower.contains("sprint") || lower.contains("schedule") || lower.contains("november")) {
            return "Release Planning & Sprint Schedule";
        }
        if (lower.contains("deployment") || lower.contains("devops") || lower.contains("failover") || lower.contains("option b")) {
            return "Deployment & High-Availability Strategy";
        }
        if (lower.contains("security") || lower.contains("audit") || lower.contains("compliance")) {
            return "Security & Compliance Review";
        }
        if (lower.contains("action") || lower.contains("assigned") || lower.contains("next steps") || lower.contains("wrap")) {
            return "Action Items & Conclusion";
        }

        return switch (index) {
            case 0 -> "Introduction & Agenda Overview";
            case 1 -> "Technical Architecture Discussion";
            case 2 -> "Implementation & Operations Review";
            case 3 -> "Milestone Planning & Timeline";
            default -> "Discussion & Next Steps";
        };
    }

    private String synthesizeChapterSummary(List<TranscriptSegment> chunk) {
        if (chunk.isEmpty()) return "Discussion on agenda topic.";
        String text = chunk.get(0).getText();
        if (text == null || text.isBlank()) return "Discussion on agenda topic.";
        return cleanBulletPoint(text);
    }

    private List<String> extractTopKeywords(String text, int limit) {
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().split("\\s+");
        Map<String, Integer> freqMap = new HashMap<>();
        for (String w : words) {
            if (w.length() > 3 && !STOP_WORDS.contains(w)) {
                freqMap.put(w, freqMap.getOrDefault(w, 0) + 1);
            }
        }
        return freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> capitalize(e.getKey()))
                .collect(Collectors.toList());
    }

    public List<ParkingQuestion> extractParkingQuestions(List<String> sentences, List<TranscriptSegment> segments) {
        List<ParkingQuestion> items = new ArrayList<>();
        long idGen = 1L;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i).trim();
            if (sentence.isEmpty()) continue;

            String lower = sentence.toLowerCase();
            boolean isQuestion = sentence.endsWith("?") ||
                    sentence.matches("(?i)^(who|what|why|how|when|where|can we|could we|should we|is there|do we|is anyone).*");

            boolean isExplicitDeferred = lower.contains("parking lot") || lower.contains("table this") ||
                    lower.contains("take this offline") || lower.contains("discuss later") ||
                    lower.contains("put in the parking") || lower.contains("table the");

            if (!isQuestion && !isExplicitDeferred) continue;

            String askedBy = "Team";
            long timestampMs = 0L;
            if (segments != null) {
                for (TranscriptSegment seg : segments) {
                    if (seg.getText() != null && (seg.getText().contains(sentence) || sentence.contains(seg.getText()))) {
                        if (seg.getSpeaker() != null && !seg.getSpeaker().isBlank() && !seg.getSpeaker().equalsIgnoreCase("unknown")) {
                            askedBy = capitalize(seg.getSpeaker());
                        }
                        timestampMs = seg.getStartMs();
                        break;
                    }
                }
            }

            String nextSentence = (i + 1 < sentences.size()) ? sentences.get(i + 1).toLowerCase() : "";
            boolean questionParkedByNext = nextSentence.contains("park that") || nextSentence.contains("table that") ||
                    nextSentence.contains("take that offline") || nextSentence.contains("parking lot for that");

            String status;
            String notes;
            if (isExplicitDeferred || questionParkedByNext) {
                status = "PARKING_LOT";
                notes = "Deferred to follow-up session or offline resolution";
            } else if (nextSentence.contains("yes") || nextSentence.contains("we decided") || nextSentence.contains("settled on") || nextSentence.contains("approved")) {
                status = "RESOLVED";
                notes = "Addressed during meeting";
            } else {
                status = "UNANSWERED";
                notes = "Pending answer by " + askedBy + " / project team";
            }

            String timeFormatted = TopicChapter.formatMs(timestampMs);
            String cleanQuestion = cleanBulletPoint(sentence);
            items.add(new ParkingQuestion(idGen++, cleanQuestion, askedBy, timestampMs, timeFormatted, status, notes));
        }

        return items;
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

        // Reading time estimation (average 200 words per minute ~ 3.3 words/second)
        int readingTime = Math.max(15, (int) Math.ceil(wordCount / 3.3));
        metrics.put("readingTimeSeconds", readingTime);

        // Basic sentiment estimation
        String lower = fullText.toLowerCase();
        int positiveCount = countOccurrences(lower, List.of("great", "good", "excellent", "agree", "progress", "success", "approved", "savings"));
        int negativeCount = countOccurrences(lower, List.of("issue", "problem", "delay", "risk", "concern", "fail", "reject", "costly"));

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
