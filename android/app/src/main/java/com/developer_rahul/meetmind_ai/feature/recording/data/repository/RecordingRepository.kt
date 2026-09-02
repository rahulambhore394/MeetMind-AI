package com.developer_rahul.meetmind_ai.feature.recording.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto
import kotlinx.coroutines.flow.SharedFlow

interface RecordingRepository {
    val recordingEvents: SharedFlow<Map<String, String>>
    suspend fun startRecording(meetingId: Long): NetworkResult<RecordingResponseDto>
    suspend fun stopRecording(meetingId: Long, recordingId: Long): NetworkResult<RecordingResponseDto>
    suspend fun listRecordings(meetingId: Long): NetworkResult<List<RecordingResponseDto>>
    suspend fun uploadRecordingFile(
        meetingId: Long,
        recordingId: Long,
        fileUri: String,
        onProgress: (Int) -> Unit
    ): NetworkResult<RecordingResponseDto>
    fun subscribeToRecordingEvents(meetingId: Long)
    fun unsubscribeFromRecordingEvents(meetingId: Long)
}
