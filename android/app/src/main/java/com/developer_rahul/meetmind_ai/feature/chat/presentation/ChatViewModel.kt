package com.developer_rahul.meetmind_ai.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.chat.data.repository.ChatRepository
import com.developer_rahul.meetmind_ai.feature.chat.domain.model.ChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val meetingId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
        observeMessages()
        chatRepository.subscribeToChat(meetingId)
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = chatRepository.getMessages(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, messages = result.data.reversed()) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {}
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatRepository.messages
                .filter { it.meetingId == meetingId }
                .collect { message ->
                    _uiState.update { state ->
                        // Deduplicate and insert at the beginning (since UI is reverseLayout = true)
                        if (state.messages.any { it.id == message.id }) state
                        else state.copy(messages = listOf(message) + state.messages)
                    }
                }
        }
    }

    fun onMessageChange(text: String) {
        _uiState.update { it.copy(pendingMessage = text) }
    }

    fun sendMessage() {
        val content = _uiState.value.pendingMessage
        if (content.isBlank()) return

        chatRepository.sendMessage(meetingId, content)
        _uiState.update { it.copy(pendingMessage = "") }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.unsubscribeFromChat(meetingId)
    }
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val pendingMessage: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
