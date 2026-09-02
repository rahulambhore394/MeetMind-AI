package com.meetmind.meetmind_backend.recording;

public enum RecordingStatus {
    STARTED,
    PROCESSING,
    AUDIO_PREPARING,
    AUDIO_CHUNKING,
    READY_FOR_TRANSCRIPTION,
    TRANSCRIPTION_PROCESSING,
    COMPLETED,
    FAILED
}
