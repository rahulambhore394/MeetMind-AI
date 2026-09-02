package com.developer_rahul.meetmind_ai.core.media.recording

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.*
import com.developer_rahul.meetmind_ai.feature.recording.data.remote.RecordingUploadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecordingUploadManager(private val context: Context) {

    fun startUpload(meetingId: Long, recordingId: Long, fileUri: String) {
        RecordingUploadWorker.start(context, meetingId, recordingId, fileUri)
    }

    fun getUploadProgress(recordingId: Long): Flow<Int?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("upload_recording_$recordingId")
            .asFlow()
            .map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                if (workInfo != null && workInfo.state == WorkInfo.State.RUNNING) {
                    workInfo.progress.getInt(RecordingUploadWorker.KEY_PROGRESS, 0)
                } else {
                    null
                }
            }
    }

    fun getUploadStatus(recordingId: Long): Flow<WorkInfo.State?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("upload_recording_$recordingId")
            .asFlow()
            .map { it.firstOrNull()?.state }
    }
}
