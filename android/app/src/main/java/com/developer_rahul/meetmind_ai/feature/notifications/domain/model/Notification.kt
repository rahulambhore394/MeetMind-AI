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
) {
    val formattedTime: String
        get() {
            return try {
                val parsed = java.time.LocalDateTime.parse(createdAt)
                val now = java.time.LocalDateTime.now()
                val diffSeconds = java.time.Duration.between(parsed, now).seconds
                when {
                    diffSeconds < 60 -> "Just now"
                    diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
                    diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
                    else -> "${diffSeconds / 86400}d ago"
                }
            } catch (e: Exception) {
                if (createdAt.length >= 16) {
                    createdAt.substring(0, 16).replace("T", " ")
                } else {
                    createdAt
                }
            }
        }

    val isMeetingRelated: Boolean
        get() = type == "MEETING_INVITATION" || type == "MEETING_STARTED" || type == "MEETING_REMINDER"

    val isAiRelated: Boolean
        get() = type.contains("AI") || type.contains("REPRESENTATIVE") || type.contains("TRANSCRIPT") || type.contains("ACTION_ITEM")
}
