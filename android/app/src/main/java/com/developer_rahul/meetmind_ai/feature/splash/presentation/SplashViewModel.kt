package com.developer_rahul.meetmind_ai.feature.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.feature.auth.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            delay(1200) // Delightful 1200ms splash to showcase brand identity & aura animations
            if (authRepository.isLoggedIn()) {
                _uiState.value = SplashUiState.Authenticated
            } else if (authRepository.hasCompletedOnboarding()) {
                _uiState.value = SplashUiState.Unauthenticated
            } else {
                _uiState.value = SplashUiState.FirstLaunch
            }
        }
    }
}
