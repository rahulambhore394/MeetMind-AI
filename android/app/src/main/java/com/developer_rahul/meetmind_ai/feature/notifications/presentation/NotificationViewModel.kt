package com.developer_rahul.meetmind_ai.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.notifications.domain.model.Notification
import com.developer_rahul.meetmind_ai.feature.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.ArrayList

enum class NotificationFilterTab {
    ALL,
    UNREAD,
    MEETINGS,
    AI_INSIGHTS
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val selectedTab: NotificationFilterTab = NotificationFilterTab.ALL,
    val unreadCount: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredNotifications: List<Notification>
        get() = when (selectedTab) {
            NotificationFilterTab.ALL -> notifications
            NotificationFilterTab.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilterTab.MEETINGS -> notifications.filter { it.isMeetingRelated }
            NotificationFilterTab.AI_INSIGHTS -> notifications.filter { it.isAiRelated }
        }
}

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        observeNotifications()
    }

    fun setFilterTab(tab: NotificationFilterTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.notifications.isEmpty(), error = null) }
            coroutineScope {
                val notificationsDeferred = async { notificationRepository.getNotifications() }
                val unreadDeferred = async { notificationRepository.getUnreadCount() }

                val result = notificationsDeferred.await()
                val unreadResult = unreadDeferred.await()

                _uiState.update { state ->
                    var s = state.copy(isLoading = false)
                    when (result) {
                        is NetworkResult.Success<List<Notification>> -> s = s.copy(notifications = result.data)
                        is NetworkResult.Error -> s = s.copy(error = result.error.message)
                        else -> {}
                    }
                    if (unreadResult is NetworkResult.Success<Long>) {
                        s = s.copy(unreadCount = unreadResult.data)
                    }
                    s
                }
            }
        }
    }

    private fun observeNotifications() {
        notificationRepository.notificationEvents
            .onEach { notification ->
                _uiState.update { state ->
                    val newList = ArrayList<Notification>()
                    newList.add(notification)
                    // avoid duplicates by ID if already present
                    for (n in state.notifications) {
                        if (n.id != notification.id) {
                            newList.add(n)
                        }
                    }
                    state.copy(notifications = newList, unreadCount = state.unreadCount + 1)
                }
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
                val updated = state.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
                state.copy(notifications = updated)
            }
            updateUnreadCount()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
            _uiState.update { state ->
                val updated = state.notifications.map { it.copy(isRead = true) }
                state.copy(notifications = updated, unreadCount = 0)
            }
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(id)
            _uiState.update { state ->
                val updated = state.notifications.filter { it.id != id }
                val wasUnread = state.notifications.find { it.id == id }?.isRead == false
                val newUnread = if (wasUnread && state.unreadCount > 0) state.unreadCount - 1 else state.unreadCount
                state.copy(notifications = updated, unreadCount = newUnread)
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAllNotifications()
            _uiState.update { state ->
                state.copy(notifications = emptyList(), unreadCount = 0)
            }
        }
    }

    fun sendTestNotification(type: String = "AI_SUMMARY_READY") {
        viewModelScope.launch {
            val result = notificationRepository.createTestNotification(type)
            if (result is NetworkResult.Success) {
                val newNotification = result.data
                _uiState.update { state ->
                    val newList = ArrayList<Notification>()
                    newList.add(newNotification)
                    newList.addAll(state.notifications)
                    state.copy(notifications = newList, unreadCount = state.unreadCount + 1)
                }
            }
        }
    }
}
