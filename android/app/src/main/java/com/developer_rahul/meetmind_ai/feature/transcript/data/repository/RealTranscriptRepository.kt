package com.developer_rahul.meetmind_ai.feature.transcript.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.transcript.data.remote.TranscriptApiService
import com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto.MeetingTranscriptDto
import com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto.TranscriptSegmentDto
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.Transcript
import com.developer_rahul.meetmind_ai.feature.transcript.domain.model.TranscriptSegment
import com.developer_rahul.meetmind_ai.feature.transcript.domain.repository.TranscriptRepository

class RealTranscriptRepository(
    private val apiService: TranscriptApiService
) : TranscriptRepository {

    override suspend fun getTranscripts(meetingId: Long): NetworkResult<List<Transcript>> {
        return try {
            val response = apiService.listTranscripts(meetingId)
            NetworkResult.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getTranscriptDetails(meetingId: Long, transcriptId: Long): NetworkResult<Transcript> {
        return try {
            val response = apiService.getTranscript(meetingId, transcriptId)
            NetworkResult.Success(response.transcript.toDomain(response.segments))
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun triggerTranscription(meetingId: Long, recordingId: Long): NetworkResult<Transcript> {
        return try {
            val response = apiService.triggerTranscription(meetingId, mapOf("recordingId" to recordingId))
            NetworkResult.Success(response.toDomain())
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    private fun MeetingTranscriptDto.toDomain(segments: List<TranscriptSegmentDto> = emptyList()): Transcript {
        return Transcript(
            id = id,
            meetingId = meetingId,
            recordingId = recordingId,
            language = language,
            status = status,
            createdAt = createdAt,
            segments = segments.map { it.toDomain() }
        )
    }

    private fun TranscriptSegmentDto.toDomain(): TranscriptSegment {
        return TranscriptSegment(
            id = id,
            speaker = speaker ?: "Unknown",
            time = formatMs(startMs),
            text = text,
            startMs = startMs
        )
    }

    private fun formatMs(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
