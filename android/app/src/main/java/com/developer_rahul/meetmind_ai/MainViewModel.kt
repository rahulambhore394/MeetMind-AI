package com.developer_rahul.meetmind_ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.core.network.websocket.MeetingWebSocketManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val tokenProvider: TokenProvider,
    private val webSocketManager: MeetingWebSocketManager
) : ViewModel() {

    private val _unauthorizedEvent = MutableSharedFlow<Unit>()
    val unauthorizedEvent = _unauthorizedEvent.asSharedFlow()

    init {
        // Handle unauthorized session expiration
        viewModelScope.launch {
            tokenProvider.unauthorizedEvent.collect {
                webSocketManager.disconnect()
                tokenProvider.clearToken()
                _unauthorizedEvent.emit(Unit)
            }
        }

        // Auto-subscribe to real-time notifications for the authenticated user
        viewModelScope.launch {
            val userId = tokenProvider.getUserId()
            if (userId > 0) {
                webSocketManager.connect()
                webSocketManager.subscribeToUserNotifications(userId)
            }
        }
    }
}
