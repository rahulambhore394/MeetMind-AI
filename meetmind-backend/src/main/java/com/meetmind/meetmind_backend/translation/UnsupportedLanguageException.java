package com.meetmind.meetmind_backend.translation;

public class UnsupportedLanguageException extends TranslationException {
    public UnsupportedLanguageException(String message) {
        super(message);
    }
}
