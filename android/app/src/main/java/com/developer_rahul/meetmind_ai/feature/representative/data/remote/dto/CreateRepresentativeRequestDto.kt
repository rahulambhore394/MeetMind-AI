package com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRepresentativeRequestDto(
    val monitoredTopics: List<String>,
    val monitoredQuestions: List<String>,
    val importantPeople: List<String>,
    val reportPreferences: String? = null,
    val notificationPreferences: String? = null
)
