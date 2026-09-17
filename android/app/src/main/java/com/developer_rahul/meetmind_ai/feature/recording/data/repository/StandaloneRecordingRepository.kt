package com.developer_rahul.meetmind_ai.feature.recording.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StandaloneRecordingRepository : RecordingRepository {

    private val _recordingEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 16)
    override val recordingEvents: SharedFlow<Map<String, String>> = _recordingEvents.asSharedFlow()

    private val sampleRecording = RecordingResponseDto(
        id = 1L,
        meetingId = 101L,
        ownerId = 101L,
        startedAt = "2026-09-04 10:00:00",
        endedAt = "2026-09-04 10:30:00",
        duration = 1800L,
        status = "COMPLETED",
        createdAt = "2026-09-04 10:00:00"
    )

    override suspend fun startRecording(meetingId: Long): NetworkResult<RecordingResponseDto> {
        return NetworkResult.Success(sampleRecording.copy(meetingId = meetingId, status = "RECORDING"))
    }

    override suspend fun stopRecording(
        meetingId: Long,
        recordingId: Long
    ): NetworkResult<RecordingResponseDto> {
        return NetworkResult.Success(sampleRecording.copy(meetingId = meetingId, id = recordingId, status = "COMPLETED"))
    }

    override suspend fun listRecordings(meetingId: Long): NetworkResult<List<RecordingResponseDto>> {
        return NetworkResult.Success(listOf(sampleRecording.copy(meetingId = meetingId)))
    }

    override suspend fun uploadRecordingFile(
        meetingId: Long,
        recordingId: Long,
        fileUri: String,
        onProgress: (Int) -> Unit
    ): NetworkResult<RecordingResponseDto> {
        onProgress(100)
        return NetworkResult.Success(sampleRecording.copy(meetingId = meetingId, id = recordingId))
    }

    override suspend fun downloadRecording(
        meetingId: Long,
        recordingId: Long,
        destinationFile: java.io.File
    ): NetworkResult<java.io.File> {
        return NetworkResult.Success(destinationFile)
    }

    override fun subscribeToRecordingEvents(meetingId: Long) {
        // No-op for standalone offline mode
    }

    override fun unsubscribeFromRecordingEvents(meetingId: Long) {
        // No-op for standalone offline mode
    }
}
