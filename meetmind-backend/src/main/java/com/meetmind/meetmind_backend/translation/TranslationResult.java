package com.meetmind.meetmind_backend.translation;

public class TranslationResult {

    private final String sourceText;
    private final String translatedText;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final String provider;

    public TranslationResult(String sourceText, String translatedText, String sourceLanguage, String targetLanguage, String provider) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.provider = provider;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public String toString() {
        return "TranslationResult{" +
                "sourceLanguage='" + sourceLanguage + '\'' +
                ", targetLanguage='" + targetLanguage + '\'' +
                ", provider='" + provider + '\'' +
                ", sourceText='" + sourceText + '\'' +
                ", translatedText='" + translatedText + '\'' +
                '}';
    }
}
