package com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PredefinedQaDto(
    val question: String,
    val answer: String
)

