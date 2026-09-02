package com.developer_rahul.meetmind_ai.core.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeetMindViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MeetMindUiState>(MeetMindUiState.Idle)
    val uiState = _uiState.asStateFlow()
}

sealed interface MeetMindUiState {
    object Idle : MeetMindUiState
    object Loading : MeetMindUiState
    object Success : MeetMindUiState
    data class Error(val message: String) : MeetMindUiState
}
