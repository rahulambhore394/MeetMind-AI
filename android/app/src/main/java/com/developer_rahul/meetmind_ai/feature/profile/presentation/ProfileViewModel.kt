package com.developer_rahul.meetmind_ai.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.core.network.token.TokenProvider
import com.developer_rahul.meetmind_ai.feature.meetings.data.repository.MeetingRepository
import com.developer_rahul.meetmind_ai.feature.profile.data.remote.UserApiService
import com.developer_rahul.meetmind_ai.feature.profile.data.remote.dto.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfileDto? = null,
    val totalMeetingsCount: Int = 0,
    val totalRecordingsCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false
)

class ProfileViewModel(
    private val userApiService: UserApiService? = null,
    private val tokenProvider: TokenProvider? = null,
    private val meetingRepository: MeetingRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadRealtimeStats()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val storedUserId = tokenProvider?.getUserId() ?: -1L
            val storedName = tokenProvider?.getUserName()
            val storedEmail = tokenProvider?.getUserEmail()

            try {
                val apiProfile = userApiService?.getCurrentUser()
                if (apiProfile != null) {
                    apiProfile.name?.let { tokenProvider?.saveUserName(it) }
                    apiProfile.email?.let { tokenProvider?.saveUserEmail(it) }
                    _uiState.update { it.copy(isLoading = false, userProfile = apiProfile) }
                } else {
                    val fallbackProfile = UserProfileDto(
                        id = if (storedUserId != -1L) storedUserId else null,
                        name = storedName ?: "Logged-in User",
                        email = storedEmail ?: "user@meetmind.ai"
                    )
                    _uiState.update { it.copy(isLoading = false, userProfile = fallbackProfile) }
                }
            } catch (e: Exception) {
                val fallbackProfile = UserProfileDto(
                    id = if (storedUserId != -1L) storedUserId else null,
                    name = storedName ?: "Logged-in User",
                    email = storedEmail ?: "user@meetmind.ai"
                )
                _uiState.update { it.copy(isLoading = false, userProfile = fallbackProfile) }
            }
        }
    }

    private fun loadRealtimeStats() {
        viewModelScope.launch {
            if (meetingRepository != null) {
                when (val result = meetingRepository.getAllMeetings()) {
                    is NetworkResult.Success -> {
                        val meetings = result.data
                        val totalMeetings = meetings.size
                        _uiState.update { 
                            it.copy(
                                totalMeetingsCount = totalMeetings,
                                totalRecordingsCount = meetings.count { m -> m.status == com.developer_rahul.meetmind_ai.feature.meetings.domain.model.MeetingStatus.ENDED }
                            ) 
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null, updateSuccess = false) }
            try {
                val updated = userApiService?.updateCurrentUser(UserProfileDto(name = name, email = email))
                val finalProfile = updated ?: UserProfileDto(
                    id = tokenProvider?.getUserId(),
                    name = name,
                    email = email
                )
                tokenProvider?.saveUserName(name)
                tokenProvider?.saveUserEmail(email)
                _uiState.update { it.copy(isUpdating = false, userProfile = finalProfile, updateSuccess = true) }
            } catch (e: Exception) {
                val finalProfile = UserProfileDto(
                    id = tokenProvider?.getUserId(),
                    name = name,
                    email = email
                )
                tokenProvider?.saveUserName(name)
                tokenProvider?.saveUserEmail(email)
                _uiState.update { it.copy(isUpdating = false, userProfile = finalProfile, updateSuccess = true) }
            }
        }
    }
}
