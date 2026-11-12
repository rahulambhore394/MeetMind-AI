package com.developer_rahul.meetmind_ai.feature.notifications.domain.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification
import kotlinx.coroutines.flow.SharedFlow

interface NotificationRepository {
    val notificationEvents: SharedFlow<Notification>
    
    suspend fun getNotifications(): NetworkResult<List<Notification>>
    suspend fun getUnreadCount(): NetworkResult<Long>
    suspend fun markAsRead(id: Long): NetworkResult<Unit>
    suspend fun markAllAsRead(): NetworkResult<Unit>
    suspend fun deleteNotification(id: Long): NetworkResult<Unit>
    suspend fun clearAllNotifications(): NetworkResult<Unit>
    suspend fun createTestNotification(type: String = "AI_SUMMARY_READY"): NetworkResult<Notification>
    suspend fun registerDevice(fcmToken: String): NetworkResult<Unit>
    suspend fun unregisterDevice(fcmToken: String): NetworkResult<Unit>
}
