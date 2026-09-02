package com.developer_rahul.meetmind_ai.feature.auth.presentation

import com.developer_rahul.meetmind_ai.core.network.model.MeetMindError
import com.developer_rahul.meetmind_ai.core.network.model.ErrorType
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.auth.data.remote.dto.LoginResponse
import com.developer_rahul.meetmind_ai.feature.auth.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun `login with empty fields sets error messages`() {
        viewModel.login()
        
        val state = viewModel.uiState.value
        assertEquals("Email is required", state.emailError)
        assertEquals("Password is required", state.passwordError)
    }

    @Test
    fun `login success updates state correctly`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")
        
        coEvery { authRepository.login(any(), any()) } returns NetworkResult.Success(
            LoginResponse("token", 1L, "Rahul", "test@example.com")
        )

        viewModel.login()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `login failure updates state with error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password")
        
        val errorMessage = "Invalid credentials"
        coEvery { authRepository.login(any(), any()) } returns NetworkResult.Error(
            MeetMindError(ErrorType.UNAUTHORIZED, errorMessage)
        )

        viewModel.login()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertEquals(errorMessage, state.error)
        assertFalse(state.isLoading)
    }
}
