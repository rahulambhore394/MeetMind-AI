package com.developer_rahul.meetmind_ai.feature.notifications.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Long,
    val userId: Long,
    val type: String,
    val title: String,
    val body: String,
    val relatedMeetingId: Long? = null,
    val relatedRepresentativeId: Long? = null,
    val isRead: Boolean,
    val createdAt: String
)
