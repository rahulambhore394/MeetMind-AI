package com.developer_rahul.meetmind_ai.feature.chat.data.remote

import com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto.ChatMessageRequestDto
import com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto.ChatMessageResponseDto
import com.developer_rahul.meetmind_ai.feature.chat.data.remote.dto.ChatPageResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {
    @GET("api/meetings/{meetingId}/messages")
    suspend fun getMessages(
        @Path("meetingId") meetingId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ChatPageResponseDto

    @POST("api/meetings/{meetingId}/messages")
    suspend fun sendMessage(
        @Path("meetingId") meetingId: Long,
        @Body request: ChatMessageRequestDto
    ): ChatMessageResponseDto
}
