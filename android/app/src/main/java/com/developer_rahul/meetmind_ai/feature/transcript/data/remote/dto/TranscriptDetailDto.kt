package com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptDetailDto(
    val transcript: MeetingTranscriptDto,
    val segments: List<TranscriptSegmentDto>
)
