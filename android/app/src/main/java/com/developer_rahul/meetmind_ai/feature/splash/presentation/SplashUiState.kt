package com.developer_rahul.meetmind_ai.feature.splash.presentation

sealed interface SplashUiState {
    object Idle : SplashUiState
    object Loading : SplashUiState
    object Authenticated : SplashUiState
    object Unauthenticated : SplashUiState
    object FirstLaunch : SplashUiState
}
