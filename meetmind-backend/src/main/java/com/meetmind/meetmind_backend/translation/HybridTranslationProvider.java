package com.meetmind.meetmind_backend.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, hybrid real-time translation provider.
 * 1. Checks in-memory fast phrase cache for zero-latency instant response.
 * 2. Queries dynamic multi-language translation engine for high-accuracy live speech.
 * 3. Gracefully falls back to LocalTranslationProvider if offline or network unavailable.
 */
@Component
@Primary
public class HybridTranslationProvider implements TranslationProvider {

    private static final Logger log = LoggerFactory.getLogger(HybridTranslationProvider.class);

    private final LocalTranslationProvider localProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // In-memory translation cache: key = "src:tgt:text" -> translated text
    private final Map<String, String> cache = new ConcurrentHashMap<>(256);

    public HybridTranslationProvider(LocalTranslationProvider localProvider, ObjectMapper objectMapper) {
        this.localProvider = localProvider;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public String providerName() {
        return "hybrid-live-translator";
    }

    @Override
    public boolean supportsLanguagePair(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null || targetLanguage == null) return false;
        String s = normalize(sourceLanguage);
        String t = normalize(targetLanguage);
        return !s.isEmpty() && !t.isEmpty();
    }

    @Override
    public TranslationResult translate(String text, String sourceLanguage, String targetLanguage)
            throws TranslationException {

        if (text == null) {
            throw new TranslationException("Input text cannot be null");
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new TranslationResult(text, text, sourceLanguage, targetLanguage, providerName());
        }

        String s = normalize(sourceLanguage);
        String t = normalize(targetLanguage);
        if (s.isEmpty()) s = "en";
        if (t.isEmpty()) t = "en";

        // Identity translation
        if (s.equalsIgnoreCase(t)) {
            return new TranslationResult(text, text, s, t, providerName());
        }

        // Cache lookup
        String cacheKey = s + ":" + t + ":" + trimmed.toLowerCase();
        String cachedTranslation = cache.get(cacheKey);
        if (cachedTranslation != null) {
            return new TranslationResult(text, cachedTranslation, s, t, "cache");
        }

        // Try Online Multi-Lingual API (Google GTX)
        try {
            String translated = callOnlineApi(trimmed, s, t);
            if (translated != null && !translated.isBlank()) {
                if (cache.size() < 2000) {
                    cache.put(cacheKey, translated);
                }
                return new TranslationResult(text, translated, s, t, "cloud-gtx");
            }
        } catch (Exception e) {
            log.debug("HybridTranslationProvider: Online translation unavailable ({}), falling back to local provider: {}",
                    e.getMessage(), trimmed);
        }

        // Offline / Graceful Local Provider Fallback
        TranslationResult fallback = localProvider.translate(text, s, t);
        return new TranslationResult(
                text,
                fallback.getTranslatedText(),
                s,
                t,
                fallback.getProvider()
        );
    }

    private String callOnlineApi(String text, String sourceLang, String targetLang) throws Exception {
        String url = String.format(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s",
                URLEncoder.encode(sourceLang, StandardCharsets.UTF_8),
                URLEncoder.encode(targetLang, StandardCharsets.UTF_8),
                URLEncoder.encode(text, StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            String body = response.body();
            JsonNode root = objectMapper.readTree(body);
            if (root.isArray() && root.size() > 0) {
                JsonNode sentences = root.get(0);
                if (sentences != null && sentences.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode sentence : sentences) {
                        if (sentence.isArray() && sentence.size() > 0) {
                            sb.append(sentence.get(0).asText());
                        }
                    }
                    String result = sb.toString().trim();
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } else {
            log.warn("HybridTranslationProvider: Online API returned status {}", response.statusCode());
        }
        return null;
    }

    private String normalize(String lang) {
        if (lang == null) return "";
        String trimmed = lang.trim().toLowerCase();
        int dashIdx = trimmed.indexOf('-');
        if (dashIdx != -1) {
            trimmed = trimmed.substring(0, dashIdx);
        }
        int underIdx = trimmed.indexOf('_');
        if (underIdx != -1) {
            trimmed = trimmed.substring(0, underIdx);
        }
        return trimmed;
    }
}
