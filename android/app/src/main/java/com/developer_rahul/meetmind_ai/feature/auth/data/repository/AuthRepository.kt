package com.developer_rahul.meetmind_ai.feature.auth.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginResponse
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.RegisterResponse

interface AuthRepository {
    suspend fun login(email: String, password: String): NetworkResult<LoginResponse>
    suspend fun register(name: String, email: String, password: String): NetworkResult<RegisterResponse>
    fun logout()
    fun isLoggedIn(): Boolean
}
