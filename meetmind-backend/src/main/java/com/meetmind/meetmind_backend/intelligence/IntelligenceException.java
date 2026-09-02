package com.meetmind.meetmind_backend.intelligence;

public class IntelligenceException extends Exception {
    public IntelligenceException(String message) {
        super(message);
    }

    public IntelligenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
