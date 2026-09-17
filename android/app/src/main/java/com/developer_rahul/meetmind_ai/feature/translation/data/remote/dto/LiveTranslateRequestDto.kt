package com.developer_rahul.meetmind_ai.feature.translation.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveTranslateRequestDto(
    val sourceText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long,
    val segmentId: Long? = null,
    val speaker: String? = null
)

@Serializable
data class LanguagePreferenceRequestDto(
    val targetLanguage: String
)
