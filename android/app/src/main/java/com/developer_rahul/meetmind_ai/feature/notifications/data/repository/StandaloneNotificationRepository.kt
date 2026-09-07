package com.developer_rahul.meetmind_ai.feature.notifications.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification
import com.developer_rahul.meetmind_ai.feature.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StandaloneNotificationRepository : NotificationRepository {

    private val _notificationEvents = MutableSharedFlow<Notification>(extraBufferCapacity = 16)
    override val notificationEvents: SharedFlow<Notification> = _notificationEvents.asSharedFlow()

    private val sampleNotifications = mutableListOf(
        Notification(
            id = 1L,
            userId = 101L,
            type = "AI_REPORT_READY",
            title = "AI Representative Report Ready",
            body = "Your representative report for 'AI Architecture Sync' is ready to view.",
            relatedMeetingId = 101L,
            relatedRepresentativeId = 1L,
            isRead = false,
            createdAt = "10 mins ago"
        ),
        Notification(
            id = 2L,
            userId = 101L,
            type = "MEETING_REMINDER",
            title = "Upcoming Meeting: Jetpack Compose Review",
            body = "Starts in 15 minutes. Join room code: MM-992-UX",
            relatedMeetingId = 102L,
            relatedRepresentativeId = null,
            isRead = true,
            createdAt = "1 hour ago"
        )
    )

    override suspend fun getNotifications(): NetworkResult<List<Notification>> {
        return NetworkResult.Success(sampleNotifications.toList())
    }

    override suspend fun getUnreadCount(): NetworkResult<Long> {
        val count = sampleNotifications.count { !it.isRead }.toLong()
        return NetworkResult.Success(count)
    }

    override suspend fun markAsRead(id: Long): NetworkResult<Unit> {
        val index = sampleNotifications.indexOfFirst { it.id == id }
        if (index != -1) {
            sampleNotifications[index] = sampleNotifications[index].copy(isRead = true)
        }
        return NetworkResult.Success(Unit)
    }

    override suspend fun markAllAsRead(): NetworkResult<Unit> {
        for (i in sampleNotifications.indices) {
            sampleNotifications[i] = sampleNotifications[i].copy(isRead = true)
        }
        return NetworkResult.Success(Unit)
    }

    override suspend fun registerDevice(fcmToken: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun unregisterDevice(fcmToken: String): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }
}
