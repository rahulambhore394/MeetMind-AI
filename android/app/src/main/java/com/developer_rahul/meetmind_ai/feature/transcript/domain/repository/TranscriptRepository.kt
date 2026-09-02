package com.developer_rahul.meetmind_ai.feature.transcript.domain.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.Transcript

interface TranscriptRepository {
    suspend fun getTranscripts(meetingId: Long): NetworkResult<List<Transcript>>
    suspend fun getTranscriptDetails(meetingId: Long, transcriptId: Long): NetworkResult<Transcript>
    suspend fun triggerTranscription(meetingId: Long, recordingId: Long): NetworkResult<Transcript>
}
