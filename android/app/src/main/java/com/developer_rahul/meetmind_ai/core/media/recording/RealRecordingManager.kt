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
        if (_state.value.status == RecordingStatus.RECORDING || _state.value.status == RecordingStatus.STARTING) return

        timerJob?.cancel()
        _state.value = RecordingState(
            status = RecordingStatus.STARTING,
            meetingId = meetingId,
            durationMs = 0L
        )

        try {
            val file = File(context.cacheDir, "recording_${meetingId}_${System.currentTimeMillis()}.mp4")
            
            val metrics = context.resources.displayMetrics
            var width = metrics.widthPixels
            var height = metrics.heightPixels
            if (width % 2 != 0) width -= 1
            if (height % 2 != 0) height -= 1
            val density = metrics.densityDpi

            mediaRecorder = createMediaRecorder(file, width, height)

            // Android Q+ requires the MediaProjection foreground service to be running BEFORE creating virtual display
            RecordingService.startService(context, isAudioOnly = false)

            mediaProjection = projectionManager.getMediaProjection(-1, projectionData)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped by system")
                    stopRecording()
                }
            }, null)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "MeetingRecording",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null, null
            )

            mediaRecorder?.start()
            
            val now = System.currentTimeMillis()
            _state.update { it.copy(
                status = RecordingStatus.RECORDING,
                localUri = file.absolutePath,
                startTime = now,
                durationMs = 0L
            ) }
            
            startTimer(now)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            _state.update { it.copy(status = RecordingStatus.FAILED, error = e.message) }
            cleanup()
        }
    }

    override fun startAudioOnlyRecording(meetingId: Long) {
        if (_state.value.status == RecordingStatus.RECORDING || _state.value.status == RecordingStatus.STARTING) return

        timerJob?.cancel()
        _state.value = RecordingState(
            status = RecordingStatus.STARTING,
            meetingId = meetingId,
            durationMs = 0L
        )

        scope.launch {
            try {
                val file = File(context.cacheDir, "recording_audio_${meetingId}_${System.currentTimeMillis()}.m4a")

                // Android 12+ requires FOREGROUND_SERVICE_TYPE_MICROPHONE to be running
                // BEFORE audio capture begins
                RecordingService.startService(context, isAudioOnly = true)
                // Give the service a moment to start foreground
                delay(300)

                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(file.absolutePath)
                recorder.prepare()
                recorder.start()
                mediaRecorder = recorder

                val now = System.currentTimeMillis()
                _state.update { it.copy(
                    status = RecordingStatus.RECORDING,
                    localUri = file.absolutePath,
                    startTime = now,
                    durationMs = 0L
                ) }

                startTimer(now)
                Log.d(TAG, "Audio-only recording started: ${file.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start audio-only recording", e)
                RecordingService.stopService(context)
                _state.update { it.copy(status = RecordingStatus.FAILED, error = e.message) }
                cleanup()
            }
        }
    }

    private fun createMediaRecorder(file: File, width: Int, height: Int): MediaRecorder {
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
        recorder.setVideoSize(width, height)
        recorder.setVideoFrameRate(30)
        recorder.setVideoEncodingBitRate(3 * 1024 * 1024)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        
        return recorder
    }

    override fun stopRecording() {
        if (_state.value.status != RecordingStatus.RECORDING && _state.value.status != RecordingStatus.PAUSED) return

        // Capture URI before any cleanup so ViewModel can snapshot it
        val capturedUri = _state.value.localUri
        _state.update { it.copy(status = RecordingStatus.STOPPING) }
        timerJob?.cancel()

        try {
            mediaRecorder?.stop()
            Log.d(TAG, "MediaRecorder stopped. File: $capturedUri (${java.io.File(capturedUri ?: "").length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }

        RecordingService.stopService(context)

        // Keep localUri in COMPLETED state so ViewModel can read it before cleanup
        _state.update { it.copy(status = RecordingStatus.COMPLETED, localUri = capturedUri) }
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
                val baseTime = _state.value.startTime ?: System.currentTimeMillis()
                startTimer(baseTime)
                _state.update { it.copy(status = RecordingStatus.RECORDING) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume", e)
            }
        }
    }

    private fun startTimer(baseTime: Long) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - baseTime
                _state.update { it.copy(durationMs = elapsed) }
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

    override fun reset() {
        timerJob?.cancel()
        timerJob = null
        cleanup()
        _state.value = RecordingState()
    }

    override fun release() {
        stopRecording()
        reset()
    }
}
