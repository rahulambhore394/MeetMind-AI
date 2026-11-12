package com.meetmind.meetmind_backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetmind.meetmind_backend.representative.dto.PredefinedQa;
import com.meetmind.meetmind_backend.transcription.TranscriptSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Centralized Google Gemini AI Service for MeetMind AI.
 * Powers:
 * 1. Meeting Intelligence (Executive Brief, Key Takeaways, Decisions, Action Items, Chapters, Sentiment)
 * 2. Autonomous AI Representative / Proxy (Persona-driven spoken responses during live meetings)
 * 3. High-Fidelity Multilingual Live Translation
 * 4. Interactive In-Meeting Q&A Assistant ("Ask Meeting AI")
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${intelligence.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${intelligence.gemini.model:${GEMINI_MODEL:gemini-3.5-flash}}")
    private String model;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Checks if a valid Google Gemini API key has been provided.
     */
    public boolean isAvailable() {
        return apiKey != null
                && !apiKey.trim().isEmpty()
                && !apiKey.contains("your-")
                && !apiKey.startsWith("${")
                && apiKey.trim().length() > 10;
    }

    public String getModel() {
        return model != null && !model.isBlank() ? model : "gemini-3.5-flash";
    }

    /**
     * Executes content generation against Google Gemini v1beta API.
     */
    public String executeGenerateContent(String prompt, String systemInstruction, boolean jsonMode, double temperature) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();

        // 1. Optional System Instruction
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            Map<String, Object> sysPart = Map.of("text", systemInstruction.trim());
            payload.put("system_instruction", Map.of("parts", List.of(sysPart)));
        }

        // 2. User prompt content
        Map<String, Object> userPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(userPart));
        payload.put("contents", List.of(content));

        // 3. Generation configuration
        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("temperature", Math.max(0.0, Math.min(1.0, temperature)));
        if (jsonMode) {
            genConfig.put("response_mime_type", "application/json");
        }
        payload.put("generationConfig", genConfig);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        String targetModel = getModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + apiKey.trim();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Gemini API call failed with status {}: {}", response.statusCode(), response.body());
            throw new IllegalStateException("Gemini API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Gemini returned empty candidate list: " + response.body());
        }

        JsonNode firstPart = candidates.get(0).path("content").path("parts").get(0);
        return firstPart.path("text").asText("");
    }

    /**
     * Synthesizes comprehensive executive meeting intelligence in structured JSON.
     */
    public String synthesizeMeetingIntelligenceJson(String fullText) throws Exception {
        String systemInstruction = """
                You are the Chief Intelligence Analyst for MeetMind AI.
                Analyze the complete meeting transcript and extract structured, actionable intelligence.
                Be definitive, precise, and concise. Avoid vague generic summaries.
                """;

        String prompt = """
                Extract comprehensive meeting intelligence from the transcript below into valid JSON.
                
                Required JSON structure:
                {
                  "summary": "Strictly 3 to 5 sentences. Must synthesize: (1) meeting purpose/agenda, (2) key challenges or discussions, (3) definitive decisions reached, (4) timeline or next action steps.",
                  "keyPoints": ["4 to 6 concise high-impact bulleted takeaways summarizing key milestones, metrics, or technical updates"],
                  "decisions": ["Array of definitive consensus resolutions (e.g. 'Approved Q4 budget increase of $50k', 'Selected PostgreSQL over MongoDB for persistence layer')"],
                  "actionItems": [
                    {
                      "description": "Specific task to complete",
                      "assignedUser": "Assignee name if mentioned or null",
                      "dueDate": "Deadline / timeline if mentioned or null",
                      "status": "PENDING"
                    }
                  ],
                  "topics": ["Key high-level discussion topics"],
                  "questions": ["Any unresolved or open questions left for follow-up"],
                  "sentiment": {
                    "overall": "POSITIVE, NEUTRAL, or CRITICAL",
                    "engagementScore": 85,
                    "notes": "Brief one-sentence sentiment overview"
                  }
                }
                
                Transcript:
                """ + fullText;

        return executeGenerateContent(prompt, systemInstruction, true, 0.2);
    }

    /**
     * Generates a spoken persona-driven response for the AI Representative during a live meeting.
     */
    public String generateProxySpokenResponse(
            String ownerName,
            List<String> monitoredTopics,
            List<String> monitoredQuestions,
            List<PredefinedQa> predefinedQa,
            String incomingMessage,
            String recentContext
    ) throws Exception {
        String systemInstruction = String.format("""
                You are the autonomous AI Representative speaking on behalf of %s in an active business meeting.
                %s is currently not in the call, and you are authorized to represent them accurately.
                
                Guidelines:
                - Always begin naturally as their representative: e.g. "Speaking for %s:", "On behalf of %s:", or "Hi team, on behalf of %s:".
                - Keep your spoken response brief, articulate, and conversational (1 to 3 sentences maximum) suitable for Text-to-Speech synthesis.
                - If the inquiry matches %s's monitored topics or predefined knowledge base, provide the correct factual answer.
                - If you do not have sufficient information, state professionally that you have logged the inquiry and %s will follow up immediately after the call.
                - Never invent false deadlines, budgets, or commitments not supported by the knowledge base.
                """, ownerName, ownerName, ownerName, ownerName, ownerName, ownerName, ownerName);

        StringBuilder sb = new StringBuilder();
        sb.append("Representative Knowledge Base:\n");
        if (monitoredTopics != null && !monitoredTopics.isEmpty()) {
            sb.append("- Monitored Topics: ").append(String.join(", ", monitoredTopics)).append("\n");
        }
        if (monitoredQuestions != null && !monitoredQuestions.isEmpty()) {
            sb.append("- Monitored Questions: ").append(String.join("; ", monitoredQuestions)).append("\n");
        }
        if (predefinedQa != null && !predefinedQa.isEmpty()) {
            sb.append("- Predefined Q&A:\n");
            for (PredefinedQa qa : predefinedQa) {
                sb.append("  * Question: ").append(qa.question()).append(" -> Answer: ").append(qa.answer()).append("\n");
            }
        }
        if (recentContext != null && !recentContext.isBlank()) {
            sb.append("\nRecent Meeting Context:\n").append(recentContext).append("\n");
        }
        sb.append("\nParticipant's Inquiry in Meeting:\n\"").append(incomingMessage).append("\"\n\n");
        sb.append("Formulate your spoken response now:");

        String response = executeGenerateContent(sb.toString(), systemInstruction, false, 0.3);
        return response != null ? response.trim() : "";
    }

    /**
     * High-accuracy conversational translation using Gemini.
     */
    public String translateLiveTranscript(String text, String sourceLanguage, String targetLanguage) throws Exception {
        String systemInstruction = "You are a professional real-time simultaneous meeting interpreter.";
        String prompt = String.format(
                "Translate the following spoken meeting text accurately from %s to %s. Preserve technical jargon, speaker emotion, and natural conversational cadence. Return ONLY the translated text without commentary:\n\n%s",
                sourceLanguage, targetLanguage, text
        );
        String translated = executeGenerateContent(prompt, systemInstruction, false, 0.1);
        return translated != null ? translated.trim() : text;
    }

    /**
     * Interactive In-Meeting Q&A ("Ask Meeting AI").
     */
    public String askMeetingAssistant(String meetingTranscript, String question) throws Exception {
        String systemInstruction = """
                You are the in-meeting AI Copilot for MeetMind AI.
                Answer participant questions accurately using ONLY the discussion transcript provided.
                If the transcript does not contain the answer, state that clearly and suggest who in the meeting might know.
                Keep answers direct, actionable, and formatted in clean markdown.
                """;

        String prompt = String.format("""
                Meeting Transcript:
                \"\"\"
                %s
                \"\"\"
                
                Question:
                %s
                """, meetingTranscript, question);

        return executeGenerateContent(prompt, systemInstruction, false, 0.2);
    }
}

