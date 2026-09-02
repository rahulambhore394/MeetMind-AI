package com.meetmind.meetmind_backend.translation;

/**
 * Speech / text translation provider abstraction.
 * Decouples translation engines (local dictionary, Vosk/Marian, LibreTranslate, Cloud)
 * from MeetMind core business logic.
 */
public interface TranslationProvider {

    /**
     * Unique identifier for this provider (e.g. "local-dict", "libre-translate").
     */
    String providerName();

    /**
     * Checks if the provider supports translating from sourceLanguage to targetLanguage.
     * Language codes follow BCP-47 / ISO 639-1 (e.g. "en", "hi", "mr").
     */
    boolean supportsLanguagePair(String sourceLanguage, String targetLanguage);

    /**
     * Translates {@code text} from {@code sourceLanguage} to {@code targetLanguage}.
     *
     * @param text           The input string to translate (must not be null)
     * @param sourceLanguage Source language code (e.g. "en", "hi", "mr")
     * @param targetLanguage Target language code (e.g. "en", "hi", "mr")
     * @return TranslationResult containing source text, translated text, and metadata
     * @throws TranslationException if an error occurs during translation
     */
    TranslationResult translate(String text, String sourceLanguage, String targetLanguage)
            throws TranslationException;
}
