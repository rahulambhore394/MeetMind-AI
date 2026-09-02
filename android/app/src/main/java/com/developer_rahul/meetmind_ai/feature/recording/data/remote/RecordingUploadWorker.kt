package com.developer_rahul.meetmind_ai.feature.recording.data.remote

import android.content.Context
import android.util.Log
import androidx.work.*
import com.developer_rahul.meetmind_ai.MeetMindApplication
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.recording.data.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordingUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository: RecordingRepository by lazy {
        (context.applicationContext as MeetMindApplication).container.recordingRepository
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val meetingId = inputData.getLong(KEY_MEETING_ID, -1L)
        val recordingId = inputData.getLong(KEY_RECORDING_ID, -1L)
        val fileUri = inputData.getString(KEY_FILE_URI)

        if (meetingId == -1L || recordingId == -1L || fileUri == null) {
            return@withContext Result.failure()
        }

        val file = java.io.File(fileUri)
        if (!file.exists() || file.length() == 0L) {
            Log.e("RecordingUploadWorker", "File does not exist or is empty: $fileUri")
            return@withContext Result.failure()
        }

        Log.d("RecordingUploadWorker", "Starting upload for meeting $meetingId, recording $recordingId, size ${file.length()} bytes")

        val result = repository.uploadRecordingFile(
            meetingId = meetingId,
            recordingId = recordingId,
            fileUri = fileUri,
            onProgress = { progress ->
                setProgressAsync(workDataOf(KEY_PROGRESS to progress))
            }
        )

        when (result) {
            is NetworkResult.Success -> {
                Log.d("RecordingUploadWorker", "Upload successful")
                Result.success()
            }
            is NetworkResult.Error -> {
                Log.e("RecordingUploadWorker", "Upload failed: ${result.error.message}")
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            else -> Result.failure()
        }
    }

    companion object {
        const val KEY_MEETING_ID = "meeting_id"
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_FILE_URI = "file_uri"
        const val KEY_PROGRESS = "progress"

        fun start(context: Context, meetingId: Long, recordingId: Long, fileUri: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<RecordingUploadWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_MEETING_ID to meetingId,
                        KEY_RECORDING_ID to recordingId,
                        KEY_FILE_URI to fileUri
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "upload_recording_$recordingId",
                ExistingWorkPolicy.KEEP,
                uploadRequest
            )
        }
    }
}
