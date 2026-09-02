package com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveTranslationDto(
    val id: Long,
    val meetingId: Long,
    val transcriptSegmentId: Long? = null,
    val sourceLanguage: String,
    val targetLanguage: String,
    val sourceText: String,
    val translatedText: String,
    val speaker: String? = null,
    val timestamp: Long
)
