package com.developer_rahul.meetmind_ai.feature.recording.domain.model

enum class RecordingStatus {
    IDLE,
    CONSENT_REQUIRED,
    REQUESTING_PERMISSION,
    STARTING,
    RECORDING,
    PAUSED,
    STOPPING,
    PROCESSING,
    AUDIO_PREPARING,
    AUDIO_CHUNKING,
    READY_FOR_TRANSCRIPTION,
    TRANSCRIPTION_PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class RecordingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val recordingId: Long? = null,
    val meetingId: Long? = null,
    val localUri: String? = null,
    val durationMs: Long = 0,
    val startTime: Long? = null,
    val progress: Int = 0,
    val error: String? = null
)
