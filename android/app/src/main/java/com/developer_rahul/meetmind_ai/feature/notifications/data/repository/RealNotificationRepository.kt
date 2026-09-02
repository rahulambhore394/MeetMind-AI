package com.developer_rahul.meetmind_ai.feature.notifications.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import com.developer_rahul.meetmind_ai.feature.notifications.data.remote.NotificationApiService
import com.developer_rahul.meetmind_ai.feature.notifications.data.remote.dto.NotificationDto
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification
import com.developer_rahul.meetmind_ai.feature.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.ArrayList
import java.util.HashMap

class RealNotificationRepository(
    private val apiService: NotificationApiService,
    private val webSocketManager: MeetingWebSocketManager
) : NotificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _notificationEvents = MutableSharedFlow<Notification>(extraBufferCapacity = 10)
    override val notificationEvents: SharedFlow<Notification> = _notificationEvents.asSharedFlow()

    init {
        webSocketManager.notifications
            .onEach { dto: NotificationDto ->
                _notificationEvents.emit(dto.toDomain())
            }
            .launchIn(scope)
    }

    override suspend fun getNotifications(): NetworkResult<List<Notification>> {
        return try {
            val response: List<NotificationDto> = apiService.getNotifications()
            val domainList = ArrayList<Notification>()
            for (dto in response) {
                domainList.add(dto.toDomain())
            }
            NetworkResult.Success(domainList as List<Notification>)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun getUnreadCount(): NetworkResult<Long> {
        return try {
            val response: Map<String, Long> = apiService.getUnreadCount()
            val count: Long = response.get("unreadCount") ?: 0L
            NetworkResult.Success(count)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun markAsRead(id: Long): NetworkResult<Unit> {
        return try {
            apiService.markAsRead(id)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun markAllAsRead(): NetworkResult<Unit> {
        return try {
            apiService.markAllAsRead()
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun registerDevice(fcmToken: String): NetworkResult<Unit> {
        return try {
            val body = HashMap<String, String>()
            body.put("fcmToken", fcmToken)
            apiService.registerDevice(body)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun unregisterDevice(fcmToken: String): NetworkResult<Unit> {
        return try {
            val body = HashMap<String, String>()
            body.put("fcmToken", fcmToken)
            apiService.unregisterDevice(body)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    private fun NotificationDto.toDomain(): Notification {
        return Notification(
            id = this.id,
            userId = this.userId,
            type = this.type,
            title = this.title,
            body = this.body,
            relatedMeetingId = this.relatedMeetingId,
            relatedRepresentativeId = this.relatedRepresentativeId,
            isRead = this.isRead,
            createdAt = this.createdAt
        )
    }
}
