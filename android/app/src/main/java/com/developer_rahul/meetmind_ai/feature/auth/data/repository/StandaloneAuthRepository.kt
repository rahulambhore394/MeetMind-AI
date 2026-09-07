package com.developer_rahul.meetmind_ai.feature.auth.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginResponse
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.RegisterResponse

class StandaloneAuthRepository(
    private val tokenProvider: TokenProvider
) : AuthRepository {

    override suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        val mockToken = "standalone_mock_jwt_token_12345"
        val mockUserId = 101L
        tokenProvider.saveToken(mockToken)
        tokenProvider.saveUserId(mockUserId)
        return NetworkResult.Success(
            LoginResponse(
                accessToken = mockToken,
                userId = mockUserId,
                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email
            )
        )
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): NetworkResult<RegisterResponse> {
        val mockToken = "standalone_mock_jwt_token_12345"
        val mockUserId = 101L
        tokenProvider.saveToken(mockToken)
        tokenProvider.saveUserId(mockUserId)
        return NetworkResult.Success(
            RegisterResponse(
                id = mockUserId,
                name = name,
                email = email
            )
        )
    }

    override fun logout() {
        tokenProvider.clearToken()
    }

    override fun isLoggedIn(): Boolean {
        return tokenProvider.hasToken()
    }
}
