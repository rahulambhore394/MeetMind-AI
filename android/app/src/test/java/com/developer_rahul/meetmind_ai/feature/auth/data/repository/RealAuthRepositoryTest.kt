package com.developer_rahul.meetmind_ai.feature.auth.data.repository

import com.developer_rahul.meetmind_ai.core.network.model.ErrorType
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.AuthApiService
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginResponse
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RealAuthRepositoryTest {

    private val authApiService = mockk<AuthApiService>()
    private val tokenProvider = mockk<TokenProvider>(relaxed = true)
    private val repository = RealAuthRepository(authApiService, tokenProvider)

    @Test
    fun `login success returns NetworkResult Success`() = runBlocking {
        // Arrange
        val expectedResponse = LoginResponse("token", 1L, "Rahul", "test@example.com")
        coEvery { authApiService.login(any()) } returns expectedResponse

        // Act
        val result = repository.login("test@example.com", "password")

        // Assert
        assertTrue(result is NetworkResult.Success)
        assertEquals(expectedResponse, (result as NetworkResult.Success).data)
    }

    @Test
    fun `login failure returns NetworkResult Error`() = runBlocking {
        // Arrange
        coEvery { authApiService.login(any()) } throws IOException()

        // Act
        val result = repository.login("test@example.com", "password")

        // Assert
        assertTrue(result is NetworkResult.Error)
        assertEquals(ErrorType.NETWORK_ERROR, (result as NetworkResult.Error).error.type)
    }
}
