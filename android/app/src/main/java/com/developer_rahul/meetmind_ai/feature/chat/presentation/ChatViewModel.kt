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
        chatRepository.subscribeToChat(meetingId)
        loadHistory()
        observeMessages()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = chatRepository.getMessages(meetingId)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val historyMessages = result.data.reversed()
                        val merged = (state.messages + historyMessages)
                            .distinctBy { if (it.id > 0L) it.id else it.text.hashCode().toLong() }
                        state.copy(isLoading = false, messages = merged)
                    }
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
                        // Deduplicate: replace optimistic local message or insert incoming
                        val existingIndex = state.messages.indexOfFirst {
                            it.id == message.id || (it.isMe && it.text == message.text)
                        }
                        if (existingIndex >= 0) {
                            val updated = state.messages.toMutableList()
                            updated[existingIndex] = message
                            state.copy(messages = updated)
                        } else {
                            state.copy(messages = listOf(message) + state.messages)
                        }
                    }
                }
        }
    }

    fun onMessageChange(text: String) {
        _uiState.update { it.copy(pendingMessage = text) }
    }

    fun sendMessage() {
        val content = _uiState.value.pendingMessage.trim()
        if (content.isBlank()) return

        val optimisticMessage = ChatMessage(
            id = System.currentTimeMillis(),
            meetingId = meetingId,
            senderId = -1L,
            senderName = "You",
            text = content,
            timestamp = "Just now",
            isMe = true
        )

        _uiState.update { state ->
            state.copy(
                pendingMessage = "",
                messages = listOf(optimisticMessage) + state.messages
            )
        }

        chatRepository.sendMessage(meetingId, content)
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
