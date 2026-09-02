package com.developer_rahul.meetmind_ai.feature.auth.data.remote

import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginRequest
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginResponse
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.RegisterRequest
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}
