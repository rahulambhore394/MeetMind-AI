package com.developer_rahul.meetmind_ai.feature.notifications.data.remote

import com.developer_rahul.meetmind_ai.feature.notifications.data.remote.dto.NotificationDto
import retrofit2.http.*

interface NotificationApiService {
    @GET("api/notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Map<String, Long>

    @PATCH("api/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Long)

    @PATCH("api/notifications/read-all")
    suspend fun markAllAsRead()

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Long)

    @DELETE("api/notifications/clear-all")
    suspend fun clearAllNotifications()

    @POST("api/notifications/test")
    suspend fun createTestNotification(@Query("type") type: String = "AI_SUMMARY_READY"): NotificationDto

    @POST("api/notifications/devices/register")
    suspend fun registerDevice(@Body body: Map<String, String>)

    @POST("api/notifications/devices/unregister")
    suspend fun unregisterDevice(@Body body: Map<String, String>)
}
