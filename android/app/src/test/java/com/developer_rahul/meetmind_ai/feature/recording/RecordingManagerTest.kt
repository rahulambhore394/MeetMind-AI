package com.developer_rahul.meetmind_ai.feature.recording

import android.content.Context
import android.content.Intent
import com.developer_rahul.meetmind_ai.core.media.recording.RealRecordingManager
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingManagerTest {

    private lateinit var context: Context
    private lateinit var recordingManager: RealRecordingManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        val mockProjectionManager = mockk<android.media.projection.MediaProjectionManager>(relaxed = true)
        every { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) } returns mockProjectionManager
        recordingManager = RealRecordingManager(context)
    }

    @Test
    fun `initial state is IDLE`() = runTest {
        assertEquals(RecordingStatus.IDLE, recordingManager.state.value.status)
    }

    @Test
    fun `stopRecording on IDLE does nothing`() = runTest {
        recordingManager.stopRecording()
        assertEquals(RecordingStatus.IDLE, recordingManager.state.value.status)
    }
}
