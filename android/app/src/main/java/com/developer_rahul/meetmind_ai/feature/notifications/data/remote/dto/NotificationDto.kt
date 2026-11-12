package com.developer_rahul.meetmind_ai.feature.notifications.data.remote.dto

import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Long = 0L,
    val userId: Long = 0L,
    val type: String = "SYSTEM",
    val title: String = "",
    val body: String = "",
    val meetingId: Long? = null,
    val relatedMeetingId: Long? = null,
    val representativeId: Long? = null,
    val relatedRepresentativeId: Long? = null,
    val isRead: Boolean = false,
    val createdAt: String = ""
) {
    fun toDomain(): Notification = Notification(
        id = id,
        userId = userId,
        type = type,
        title = title,
        body = body,
        relatedMeetingId = relatedMeetingId ?: meetingId,
        relatedRepresentativeId = relatedRepresentativeId ?: representativeId,
        isRead = isRead,
        createdAt = createdAt
    )
}
