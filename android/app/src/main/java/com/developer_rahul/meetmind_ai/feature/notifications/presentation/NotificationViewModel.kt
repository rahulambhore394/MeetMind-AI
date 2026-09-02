package com.developer_rahul.meetmind_ai.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification
import com.developer_rahul.meetmind_ai.feature.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.ArrayList

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        observeNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = notificationRepository.getNotifications()
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, notifications = result.data) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            updateUnreadCount()
        }
    }

    private fun observeNotifications() {
        notificationRepository.notificationEvents
            .onEach { notification ->
                _uiState.update { state ->
                    val newList = ArrayList<Notification>()
                    newList.add(notification)
                    newList.addAll(state.notifications)
                    state.copy(notifications = newList)
                }
                updateUnreadCount()
            }
            .launchIn(viewModelScope)
    }

    private fun updateUnreadCount() {
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount()
            if (result is NetworkResult.Success) {
                _uiState.update { it.copy(unreadCount = result.data) }
            }
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
            _uiState.update { state ->
                val currentNotifications = state.notifications
                val newList = ArrayList<Notification>()
                for (n in currentNotifications) {
                    if (n.id == id) {
                        newList.add(n.copy(isRead = true))
                    } else {
                        newList.add(n)
                    }
                }
                state.copy(notifications = newList)
            }
            updateUnreadCount()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
            _uiState.update { state ->
                val currentNotifications = state.notifications
                val newList = ArrayList<Notification>()
                for (n in currentNotifications) {
                    newList.add(n.copy(isRead = true))
                }
                state.copy(notifications = newList)
            }
            updateUnreadCount()
        }
    }
}

data class NotificationUiState(
    val notifications: List<Notification> = ArrayList<Notification>(),
    val unreadCount: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
