package com.meetmind.meetmind_backend.transcription;

import java.nio.file.Path;

/**
 * Abstraction over speech-to-text providers.
 *
 * Implementations:
 *   - VoskTranscriptionProvider  (offline, free, initial)
 *   - WhisperTranscriptionProvider (future, cloud, OpenAI API)
 *   - AssemblyAiTranscriptionProvider (future, cloud)
 *
 * The active provider is selected via application property:
 *   transcription.provider=vosk
 */
public interface TranscriptionProvider {

    /**
     * Human-readable provider name for logging/diagnostics.
     */
    String providerName();

    /**
     * Returns true if the provider supports the given BCP-47 language tag
     * (e.g. "en", "hi", "mr").
     */
    boolean supportsLanguage(String language);

    /**
     * Transcribes the audio file at {@code audioPath} using the given language.
     *
     * @param audioPath absolute path to the audio/video file on disk
     * @param language  BCP-47 language code (e.g. "en", "hi", "mr")
     * @return structured transcription result with timestamped segments
     * @throws TranscriptionException on provider-level failure (model missing, corrupt audio, etc.)
     */
    TranscriptionResult transcribe(Path audioPath, String language) throws TranscriptionException;
}
