package com.developer_rahul.meetmind_ai.feature.notifications.domain.model

data class Notification(
    val id: Long,
    val userId: Long,
    val type: String,
    val title: String,
    val body: String,
    val relatedMeetingId: Long?,
    val relatedRepresentativeId: Long?,
    val isRead: Boolean,
    val createdAt: String
)
