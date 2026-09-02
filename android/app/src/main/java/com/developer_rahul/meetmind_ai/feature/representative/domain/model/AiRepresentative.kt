package com.developer_rahul.meetmind_ai.feature.representative.domain.model

data class AiRepresentative(
    val id: Long,
    val ownerId: Long,
    val meetingId: Long,
    val status: String,
    val mediaStoragePath: String?,
    val monitoredTopics: List<String>,
    val monitoredQuestions: List<String>,
    val importantPeople: List<String>,
    val reportPreferences: String,
    val notificationPreferences: String,
    val consentDisclosure: Boolean,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String
)
