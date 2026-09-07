package com.developer_rahul.meetmind_ai.feature.transcript.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.Transcript
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment
import com.developer_rahul.meetmind_ai.feature.transcript.domain.repository.TranscriptRepository

class StandaloneTranscriptRepository : TranscriptRepository {

    private val sampleTranscript = Transcript(
        id = 1L,
        meetingId = 101L,
        recordingId = 1L,
        language = "en",
        status = "COMPLETED",
        createdAt = "2026-09-04 23:25:00",
        segments = listOf(
            TranscriptSegment(id = 1L, speaker = "Rahul Ambhore", time = "00:00", text = "Welcome everyone to MeetMind AI standalone review.", startMs = 0L),
            TranscriptSegment(id = 2L, speaker = "Sarah Connor", time = "00:04", text = "We have disconnected the backend dependencies from Android.", startMs = 4800L),
            TranscriptSegment(id = 3L, speaker = "Alex Rivera", time = "00:09", text = "All screens now load instantly offline with clean Jetpack Compose mock states!", startMs = 9500L)
        )
    )

    override suspend fun getTranscripts(meetingId: Long): NetworkResult<List<Transcript>> {
        return NetworkResult.Success(listOf(sampleTranscript.copy(meetingId = meetingId)))
    }

    override suspend fun getTranscriptDetails(
        meetingId: Long,
        transcriptId: Long
    ): NetworkResult<Transcript> {
        return NetworkResult.Success(sampleTranscript.copy(meetingId = meetingId, id = transcriptId))
    }

    override suspend fun triggerTranscription(
        meetingId: Long,
        recordingId: Long
    ): NetworkResult<Transcript> {
        return NetworkResult.Success(sampleTranscript.copy(meetingId = meetingId, recordingId = recordingId))
    }
}
