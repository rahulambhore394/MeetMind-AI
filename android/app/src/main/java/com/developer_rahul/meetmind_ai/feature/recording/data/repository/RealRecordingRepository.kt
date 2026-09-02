package com.developer_rahul.meetmind_ai.feature.recording.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.RecordingApiService
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.*
import java.io.File

class RealRecordingRepository(
    private val recordingApiService: RecordingApiService,
    private val webSocketManager: MeetingWebSocketManager
) : RecordingRepository {

    override val recordingEvents: SharedFlow<Map<String, String>> = webSocketManager.recordingEvents

    override suspend fun startRecording(meetingId: Long): NetworkResult<RecordingResponseDto> {
        return try {
            val response = recordingApiService.startRecording(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun stopRecording(meetingId: Long, recordingId: Long): NetworkResult<RecordingResponseDto> {
        return try {
            val response = recordingApiService.stopRecording(meetingId, recordingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun listRecordings(meetingId: Long): NetworkResult<List<RecordingResponseDto>> {
        return try {
            val response = recordingApiService.listRecordings(meetingId)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun uploadRecordingFile(
        meetingId: Long,
        recordingId: Long,
        fileUri: String,
        onProgress: (Int) -> Unit
    ): NetworkResult<RecordingResponseDto> = withContext(Dispatchers.IO) {
        try {
            val file = File(fileUri)
            val requestFile = ProgressRequestBody(file, "video/mp4".toMediaTypeOrNull(), onProgress)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = recordingApiService.uploadRecordingFile(meetingId, recordingId, body)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override fun subscribeToRecordingEvents(meetingId: Long) {
        webSocketManager.subscribeToRecordings(meetingId)
    }

    override fun unsubscribeFromRecordingEvents(meetingId: Long) {
        webSocketManager.unsubscribeFromRecordings(meetingId)
    }

    private class ProgressRequestBody(
        private val file: File,
        private val contentType: okhttp3.MediaType?,
        private val onProgress: (Int) -> Unit
    ) : RequestBody() {
        override fun contentType() = contentType
        override fun contentLength() = file.length()
        override fun writeTo(sink: BufferedSink) {
            file.source().use { source ->
                val total = contentLength()
                var uploaded = 0L
                val buffer = Buffer()
                var read: Long
                while (source.read(buffer, 8192L).also { read = it } != -1L) {
                    sink.write(buffer, read)
                    uploaded += read
                    onProgress(((uploaded * 100) / total).toInt())
                }
            }
        }
    }
}
