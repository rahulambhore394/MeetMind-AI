package com.developer_rahul.meetmind_ai.feature.transcript.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptSegmentDto(
    val id: Long,
    val transcriptId: Long,
    val segmentIndex: Int,
    val speaker: String? = null,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Double? = null
)
