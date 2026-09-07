package com.developer_rahul.meetmind_ai.feature.auth.data.repository

import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.AuthApiService
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginRequest
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginResponse
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.RegisterRequest
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.RegisterResponse

class RealAuthRepository(
    private val authApiService: AuthApiService,
    private val tokenProvider: TokenProvider
) : AuthRepository {

    override suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            tokenProvider.saveToken(response.accessToken)
            tokenProvider.saveUserId(response.userId)
            tokenProvider.saveUserName(response.name)
            tokenProvider.saveUserEmail(response.email)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): NetworkResult<RegisterResponse> {
        return try {
            val response = authApiService.register(RegisterRequest(name, email, password))
            tokenProvider.saveUserId(response.id)
            tokenProvider.saveUserName(response.name)
            tokenProvider.saveUserEmail(response.email)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(ErrorMapper.mapToMeetMindError(e))
        }
    }

    override fun logout() {
        tokenProvider.clearToken()
    }

    override fun isLoggedIn(): Boolean {
        return tokenProvider.hasToken()
    }
}
