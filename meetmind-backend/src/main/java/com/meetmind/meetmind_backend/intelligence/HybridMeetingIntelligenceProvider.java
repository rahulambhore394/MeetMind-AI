package com.meetmind.meetmind_backend.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.transcription.TranscriptSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Hybrid Meeting Intelligence Provider:
 * 1. Generative LLMs (Google Gemini API / OpenAI API) synthesize 30-60 min conversations
 *    into a concise 3-5 sentence executive brief accompanied by bulleted takeaways.
 * 2. Graceful automatic fallback to LocalMeetingIntelligenceProvider if offline,
 *    unconfigured, or on rate-limit/network errors.
 */
@Component
@Primary
public class HybridMeetingIntelligenceProvider implements MeetingIntelligenceProvider {

    private static final Logger log = LoggerFactory.getLogger(HybridMeetingIntelligenceProvider.class);

    private final LocalMeetingIntelligenceProvider localProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${intelligence.provider:hybrid}")
    private String providerMode;

    @Value("${intelligence.gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${intelligence.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${intelligence.openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${intelligence.openai.model:gpt-4o-mini}")
    private String openaiModel;

    public HybridMeetingIntelligenceProvider(
            LocalMeetingIntelligenceProvider localProvider,
            ObjectMapper objectMapper
    ) {
        this.localProvider = localProvider;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public String providerName() {
        if (hasGeminiKey()) return "gemini-llm";
        if (hasOpenAiKey()) return "openai-llm";
        return "local-heuristic";
    }

    @Override
    public boolean supportsLanguage(String language) {
        return localProvider.supportsLanguage(language);
    }

    @Override
    public IntelligenceResult analyze(String fullText, List<TranscriptSegment> segments, String language) throws IntelligenceException {
        if (fullText != null && fullText.contains("FAIL_PROVIDER")) {
            throw new IntelligenceException("Simulated AI provider failure for testing");
        }

        if (fullText == null || fullText.trim().isEmpty()) {
            return localProvider.analyze(fullText, segments, language);
        }

        if (!"local".equalsIgnoreCase(providerMode)) {
            // Attempt 1: Gemini Generative AI
            if (hasGeminiKey()) {
                try {
                    log.info("Synthesizing executive brief using Gemini LLM (model: {})...", geminiModel);
                    IntelligenceResult result = callGeminiLlm(fullText, segments);
                    if (result != null && result.summary() != null && !result.summary().isBlank()) {
                        log.info("Gemini LLM executive synthesis succeeded with {} key points", result.keyPoints().size());
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("Gemini LLM synthesis failed ({}), evaluating fallback...", e.getMessage());
                }
            }

            // Attempt 2: OpenAI API
            if (hasOpenAiKey()) {
                try {
                    log.info("Synthesizing executive brief using OpenAI LLM (model: {})...", openaiModel);
                    IntelligenceResult result = callOpenAiLlm(fullText, segments);
                    if (result != null && result.summary() != null && !result.summary().isBlank()) {
                        log.info("OpenAI LLM executive synthesis succeeded");
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("OpenAI LLM synthesis failed ({}), evaluating fallback...", e.getMessage());
                }
            }
        }

        // Reliable fallback to enhanced local executive synthesizer
        log.info("Using enhanced local executive synthesizer for 3-5 sentence brief and bullet takeaways");
        return localProvider.analyze(fullText, segments, language);
    }

    private boolean hasGeminiKey() {
        return geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("your-");
    }

    private boolean hasOpenAiKey() {
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty() && !openaiApiKey.contains("your-");
    }

    private IntelligenceResult callGeminiLlm(String fullText, List<TranscriptSegment> segments) throws Exception {
        String prompt = buildExecutivePrompt(fullText);

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
                "response_mime_type", "application/json",
                "temperature", 0.2
        );
        Map<String, Object> payload = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );

        String jsonPayload = objectMapper.writeValueAsString(payload);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey.trim();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Gemini API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No candidates returned from Gemini");
        }

        String rawJson = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        return parseLlmOutput(rawJson, fullText, segments, "gemini-llm", "Google Gemini " + geminiModel);
    }

    private IntelligenceResult callOpenAiLlm(String fullText, List<TranscriptSegment> segments) throws Exception {
        String prompt = buildExecutivePrompt(fullText);

        Map<String, Object> sysMsg = Map.of("role", "system", "content", "You are an executive assistant that extracts meeting intelligence into JSON.");
        Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);
        Map<String, Object> responseFormat = Map.of("type", "json_object");

        Map<String, Object> payload = Map.of(
                "model", openaiModel,
                "messages", List.of(sysMsg, userMsg),
                "response_format", responseFormat,
                "temperature", 0.2
        );

        String jsonPayload = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey.trim())
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String rawJson = root.path("choices").get(0).path("message").path("content").asText();
        return parseLlmOutput(rawJson, fullText, segments, "openai-llm", "OpenAI " + openaiModel);
    }

    private String buildExecutivePrompt(String fullText) {
        return """
                You are an executive meeting intelligence analyst.
                Synthesize the following meeting transcript into a concise executive brief that allows executives to grasp the outcome in under 60 seconds without listening to the entire recording.
                
                Requirements:
                1. "summary": Strictly 3 to 5 sentences. Must synthesize: (a) meeting objective/context, (b) key metrics/challenge, (c) decision reached, (d) deployment or next steps timeline. Example style: "The engineering team reviewed the Q3 cloud infrastructure budget. Due to a 25% increase in compute usage, the team decided to migrate from On-Demand instances to AWS Savings Plans. Deployment is slated for Friday."
                2. "keyPoints": 4 to 6 concise bulleted takeaways.
                3. "decisions": Array of definitive consensus moments and agreed decisions (e.g. scanning linguistic cues like "We settled on", "Approved", "Let's go with Option B", "Agreed on", "Selected ... over"). Format each decision as an authoritative resolution statement that eliminates ambiguity (e.g. "Selected PostgreSQL 15 over MongoDB for the meeting intelligence persistence layer.", "Agreed on a bi-weekly release sprint schedule starting November 1st.").
                4. "actionItems": Array of objects extracted by scanning linguistic cues (e.g., "I will prepare", "assigned to", "due next Monday") with {"description": "concise task statement", "assignedUser": "assigned participant name", "dueDate": "deadline or due date", "status": "PENDING"}.
                5. "topics": Array of key subject topics.
                6. "questions": Array of open/unresolved questions.
                
                Respond ONLY with a valid JSON object matching the keys above.
                
                Transcript:
                """ + fullText;
    }

    private IntelligenceResult parseLlmOutput(
            String rawJson,
            String fullText,
            List<TranscriptSegment> segments,
            String providerName,
            String modelMetadata
    ) throws Exception {
        String cleanedJson = cleanJsonFences(rawJson);
        JsonNode node = objectMapper.readTree(cleanedJson);

        String summary = node.path("summary").asText("");
        List<String> keyPoints = readStringList(node.path("keyPoints"));
        List<String> decisions = readStringList(node.path("decisions"));
        List<String> topics = readStringList(node.path("topics"));
        List<String> questions = readStringList(node.path("questions"));

        List<ActionItemData> actionItems = new ArrayList<>();
        JsonNode actionsNode = node.path("actionItems");
        if (actionsNode.isArray()) {
            for (JsonNode a : actionsNode) {
                String desc = a.path("description").asText("");
                String user = a.hasNonNull("assignedUser") ? a.path("assignedUser").asText() : null;
                String due = a.hasNonNull("dueDate") ? a.path("dueDate").asText() : null;
                String status = a.hasNonNull("status") ? a.path("status").asText() : "PENDING";
                actionItems.add(new ActionItemData(desc, user, due, 0.95, status));
            }
        }

        List<String> sentences = localProvider.splitSentences(fullText);
        List<TopicChapter> chapters = localProvider.extractChapters(fullText, sentences, segments);
        if (topics.isEmpty() && !chapters.isEmpty()) {
            topics = chapters.stream().map(c -> c.timeRange() + ": " + c.title()).toList();
        }

        List<ParkingQuestion> parkingLotItems = localProvider.extractParkingQuestions(sentences, segments);
        if (questions.isEmpty() && !parkingLotItems.isEmpty()) {
            questions = parkingLotItems.stream().map(q -> "[" + q.status() + "] " + q.question()).toList();
        }

        // Analysis metrics
        Map<String, Object> metrics = new HashMap<>();
        int wordCount = fullText.split("\\s+").length;
        metrics.put("wordCount", wordCount);
        metrics.put("readingTimeSeconds", Math.max(15, (int) Math.ceil(wordCount / 3.3)));
        metrics.put("segmentCount", segments != null ? segments.size() : 0);
        metrics.put("provider", providerName);
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
                providerName,
                modelMetadata,
                chapters,
                parkingLotItems
        );
    }

    private List<String> readStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    list.add(text.trim());
                }
            }
        }
        return list;
    }

    private String cleanJsonFences(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
