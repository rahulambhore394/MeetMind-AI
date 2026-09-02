package com.developer_rahul.meetmind_ai.core.media.recording

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingState
import com.developer_rahul.meetmind_ai.feature.recording.domain.model.RecordingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class RealRecordingManager(
    private val context: Context
) : RecordingManager {

    private val TAG = "RecordingManager"
    private val _state = MutableStateFlow(RecordingState())
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null

    private val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    override fun startRecording(meetingId: Long, projectionData: Intent) {
        if (_state.value.status != RecordingStatus.IDLE) return

        _state.update { it.copy(status = RecordingStatus.STARTING, meetingId = meetingId) }

        try {
            val file = File(context.cacheDir, "recording_${meetingId}_${System.currentTimeMillis()}.mp4")
            
            mediaRecorder = createMediaRecorder(file)
            mediaProjection = projectionManager.getMediaProjection(-1, projectionData)
            
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "MeetingRecording",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null, null
            )

            mediaRecorder?.start()
            
            RecordingService.startService(context)
            
            _state.update { it.copy(
                status = RecordingStatus.RECORDING,
                localUri = file.absolutePath,
                startTime = System.currentTimeMillis()
            ) }
            
            startTimer()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _state.update { it.copy(status = RecordingStatus.FAILED, error = e.message) }
            cleanup()
        }
    }

    private fun createMediaRecorder(file: File): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setVideoSize(1280, 720)
        recorder.setVideoFrameRate(30)
        recorder.setVideoEncodingBitRate(3 * 1024 * 1024)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        
        return recorder
    }

    override fun stopRecording() {
        if (_state.value.status != RecordingStatus.RECORDING && _state.value.status != RecordingStatus.PAUSED) return

        _state.update { it.copy(status = RecordingStatus.STOPPING) }
        
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }
        
        RecordingService.stopService(context)
        
        timerJob?.cancel()
        _state.update { it.copy(status = RecordingStatus.COMPLETED) }
        cleanup()
    }

    override fun pauseRecording() {
        if (_state.value.status == RecordingStatus.RECORDING) {
            try {
                mediaRecorder?.pause()
                timerJob?.cancel()
                _state.update { it.copy(status = RecordingStatus.PAUSED) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause", e)
            }
        }
    }

    override fun resumeRecording() {
        if (_state.value.status == RecordingStatus.PAUSED) {
            try {
                mediaRecorder?.resume()
                startTimer()
                _state.update { it.copy(status = RecordingStatus.RECORDING) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume", e)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val baseTime = _state.value.startTime ?: System.currentTimeMillis()
            while (true) {
                delay(1000)
                _state.update { it.copy(durationMs = System.currentTimeMillis() - baseTime) }
            }
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    override fun release() {
        stopRecording()
    }
}
