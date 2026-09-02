package com.developer_rahul.meetmind_ai.feature.transcript.domain.model

data class Transcript(
    val id: Long,
    val meetingId: Long,
    val recordingId: Long,
    val language: String,
    val status: String,
    val createdAt: String,
    val segments: List<TranscriptSegment> = emptyList()
)

data class TranscriptSegment(
    val id: Long,
    val speaker: String,
    val time: String,
    val text: String,
    val startMs: Long
)
