package com.meetmind.meetmind_backend.transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline, free speech-to-text provider using Vosk.
 *
 * Language support (with corresponding free models from https://alphacephei.com/vosk/models):
 *   - "en"  → vosk-model-small-en-us   (~40MB) or vosk-model-en-us-0.22 (~1.8GB for higher accuracy)
 *   - "hi"  → vosk-model-hi-0.22       (~1.5GB)
 *   - "mr"  → vosk-model-mr-0.22       (check availability — currently community model)
 *
 * Model paths are configured via:
 *   transcription.vosk.model-path.en=models/vosk/en
 *   transcription.vosk.model-path.hi=models/vosk/hi
 *   transcription.vosk.model-path.mr=models/vosk/mr
 *
 * If the model directory does not exist at runtime, transcription returns a graceful
 * FAILED state with reason MODEL_NOT_FOUND. The server does NOT crash.
 *
 * Accuracy limitations:
 *   - English:  ~85-92% WER on clean audio with standard accent
 *   - Hindi:    ~75-85% WER depending on accent and audio quality
 *   - Marathi:  ~70-80% WER — community model, less data than en/hi
 *   - Background noise, overlapping speakers, or accents reduce accuracy significantly
 *   - No speaker diarization — all segments use speaker=null
 *   - Vosk operates on raw PCM audio; video files (.mp4) are automatically decoded
 *     via Java Sound API (requires supported codec) or will fail with TranscriptionException
 */
@Component
@ConditionalOnProperty(name = "transcription.provider", havingValue = "vosk", matchIfMissing = true)
public class VoskTranscriptionProvider implements TranscriptionProvider {

    private static final Logger log = LoggerFactory.getLogger(VoskTranscriptionProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Sample rate Vosk expects (must match model)
    private static final float SAMPLE_RATE = 16000.0f;

    @Value("${transcription.vosk.model-path.en:models/vosk/en}")
    private String modelPathEn;

    @Value("${transcription.vosk.model-path.hi:models/vosk/hi}")
    private String modelPathHi;

    @Value("${transcription.vosk.model-path.mr:models/vosk/mr}")
    private String modelPathMr;

    @Override
    public String providerName() {
        return "vosk";
    }

    @Override
    public boolean supportsLanguage(String language) {
        return language != null &&
                (language.startsWith("en") || language.startsWith("hi") || language.startsWith("mr"));
    }

    @Override
    public TranscriptionResult transcribe(Path audioPath, String language) throws TranscriptionException {
        if (audioPath == null || !Files.exists(audioPath)) {
            throw new TranscriptionException("Audio file not found: " + audioPath);
        }

        if (!supportsLanguage(language)) {
            throw new TranscriptionException("Unsupported language: " + language +
                    ". Vosk provider supports: en, hi, mr");
        }

        String modelPath = resolveModelPath(language);

        if (!Files.exists(Path.of(modelPath))) {
            throw new TranscriptionException("MODEL_NOT_FOUND: Vosk model for language '" + language +
                    "' not found at path: " + modelPath +
                    ". Download the model from https://alphacephei.com/vosk/models and extract to " + modelPath);
        }

        log.info("VoskProvider: Starting transcription — language={}, file={}, model={}", language, audioPath, modelPath);

        try {
            return doTranscribe(audioPath, modelPath);
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            throw new TranscriptionException("Transcription failed for file: " + audioPath + " — " + e.getMessage(), e);
        }
    }

    private String resolveModelPath(String language) {
        if (language.startsWith("hi")) return modelPathHi;
        if (language.startsWith("mr")) return modelPathMr;
        return modelPathEn;
    }

    private TranscriptionResult doTranscribe(Path audioPath, String modelPath)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException, TranscriptionException {

        List<TranscriptionResult.SegmentData> segments = new ArrayList<>();

        try (Model model = new Model(modelPath)) {
            AudioInputStream rawStream = AudioSystem.getAudioInputStream(audioPath.toFile());
            AudioFormat baseFormat = rawStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    SAMPLE_RATE,
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    SAMPLE_RATE,
                    false
            );

            AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, rawStream);

            try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
                // Request word-level timestamps from Vosk
                recognizer.setWords(true);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = decodedStream.read(buffer)) >= 0) {
                    recognizer.acceptWaveForm(buffer, bytesRead);
                }

                // Process final result
                String finalJson = recognizer.getFinalResult();
                segments.addAll(parseVoskResult(finalJson, segments.size()));
            }

            rawStream.close();
            decodedStream.close();
        }

        if (segments.isEmpty()) {
            log.warn("VoskProvider: No speech detected in file {}", audioPath);
        } else {
            log.info("VoskProvider: Transcription complete — {} segments produced", segments.size());
        }

        return new TranscriptionResult(segments);
    }

    /**
     * Parses Vosk's word-level JSON output into SegmentData objects.
     *
     * Vosk final result format (with setWords(true)):
     * {
     *   "result": [
     *     { "conf": 0.98, "end": 1.23, "start": 0.87, "word": "hello" },
     *     { "conf": 0.95, "end": 1.80, "start": 1.26, "word": "world" }
     *   ],
     *   "text": "hello world"
     * }
     *
     * Words are grouped into segments of up to 10 words to avoid single-word entries.
     */
    private List<TranscriptionResult.SegmentData> parseVoskResult(String json, int startIndex)
            throws IOException, TranscriptionException {

        List<TranscriptionResult.SegmentData> segments = new ArrayList<>();
        if (json == null || json.isBlank()) return segments;

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException e) {
            throw new TranscriptionException("Failed to parse Vosk JSON output: " + json, e);
        }

        JsonNode wordArray = root.get("result");
        if (wordArray == null || !wordArray.isArray() || wordArray.isEmpty()) {
            // No word-level data — fall back to full text as single segment
            String text = root.path("text").asText("").trim();
            if (!text.isBlank()) {
                segments.add(new TranscriptionResult.SegmentData(text, 0L, 0L, null, null));
            }
            return segments;
        }

        // Group words into segments (up to 10 words per segment)
        int wordsPerSegment = 10;
        int totalWords = wordArray.size();

        for (int i = 0; i < totalWords; i += wordsPerSegment) {
            int end = Math.min(i + wordsPerSegment, totalWords);
            StringBuilder textBuilder = new StringBuilder();
            long segStartMs = 0;
            long segEndMs = 0;
            double totalConf = 0.0;
            int wordCount = 0;

            for (int j = i; j < end; j++) {
                JsonNode word = wordArray.get(j);
                textBuilder.append(word.path("word").asText("")).append(" ");
                double startSec = word.path("start").asDouble(0.0);
                double endSec = word.path("end").asDouble(0.0);
                if (j == i) segStartMs = Math.round(startSec * 1000);
                segEndMs = Math.round(endSec * 1000);
                totalConf += word.path("conf").asDouble(1.0);
                wordCount++;
            }

            double avgConf = wordCount > 0 ? totalConf / wordCount : null;
            String text = textBuilder.toString().trim();

            if (!text.isBlank()) {
                segments.add(new TranscriptionResult.SegmentData(
                        text, segStartMs, segEndMs, null, avgConf
                ));
            }
        }

        return segments;
    }
}
