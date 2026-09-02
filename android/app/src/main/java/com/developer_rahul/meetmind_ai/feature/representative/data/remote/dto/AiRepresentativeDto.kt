package com.developer_rahul.meetmind_ai.feature.representative.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiRepresentativeDto(
    val id: Long,
    val ownerId: Long,
    val meetingId: Long,
    val status: String,
    val mediaStoragePath: String? = null,
    val monitoredTopicsJson: String? = null,
    val monitoredQuestionsJson: String? = null,
    val importantPeopleJson: String? = null,
    val reportPreferencesJson: String? = null,
    val notificationPreferencesJson: String? = null,
    val consentDisclosure: Boolean,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)
