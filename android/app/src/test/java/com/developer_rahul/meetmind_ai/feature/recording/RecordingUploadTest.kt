package com.developer_rahul.meetmind_ai.feature.recording

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.RecordingApiService
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.dto.RecordingResponseDto
import com.developer_rahul.meetmind_ai.feature.recording.data.repository.RealRecordingRepository
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RecordingUploadTest {

    private lateinit var repository: RealRecordingRepository
    private val apiService: RecordingApiService = mockk()
    private val webSocketManager: MeetingWebSocketManager = mockk()

    @Before
    fun setup() {
        repository = RealRecordingRepository(apiService, webSocketManager)
    }

    @Test
    fun `uploadRecordingFile returns success when api succeeds`() = runTest {
        val meetingId = 1L
        val recordingId = 100L
        val file = File.createTempFile("test", ".mp4")
        val response = RecordingResponseDto(
            id = recordingId, meetingId = meetingId, ownerId = 1,
            startedAt = "", status = "COMPLETED", createdAt = ""
        )

        coEvery { 
            apiService.uploadRecordingFile(eq(meetingId), eq(recordingId), any()) 
        } returns response

        val result = repository.uploadRecordingFile(meetingId, recordingId, file.absolutePath) { }

        assertTrue(result is NetworkResult.Success)
        file.delete()
    }
}
