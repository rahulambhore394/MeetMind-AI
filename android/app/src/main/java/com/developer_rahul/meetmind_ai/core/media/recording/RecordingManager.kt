package com.developer_rahul.meetmind_ai.core.media.recording

import android.content.Context
import android.content.Intent
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingState
import kotlinx.coroutines.flow.StateFlow

interface RecordingManager {
    val state: StateFlow<RecordingState>
    
    fun startRecording(meetingId: Long, projectionData: Intent)
    fun startAudioOnlyRecording(meetingId: Long)
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()
    fun reset()
    fun release()
}
