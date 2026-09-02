package com.developer_rahul.meetmind_ai.feature.translation.domain.model

data class Subtitle(
    val id: Long,
    val meetingId: Long,
    val speaker: String,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long
)
