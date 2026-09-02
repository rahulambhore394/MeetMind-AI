package com.meetmind.meetmind_backend.transcription;

public class TranscriptionException extends Exception {

    public TranscriptionException(String message) {
        super(message);
    }

    public TranscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
