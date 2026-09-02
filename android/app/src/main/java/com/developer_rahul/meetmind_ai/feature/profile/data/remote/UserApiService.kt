package com.developer_rahul.meetmind_ai.feature.profile.data.remote

import com.developer_rahul.meetmind_ai.feature.profile.data.remote.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApiService {
    @GET("api/users/me")
    suspend fun getCurrentUser(): UserProfileDto

    @PUT("api/users/me")
    suspend fun updateCurrentUser(@Body profile: UserProfileDto): UserProfileDto
}
