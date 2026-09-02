package com.developer_rahul.meetmind_ai.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.error.ErrorMapper
import com.developer_rahul.meetmind_ai.feature.profile.data.remote.UserApiService
import com.developer_rahul.meetmind_ai.feature.profile.data.remote.dto.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfileDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false
)

class ProfileViewModel(
    private val userApiService: UserApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = userApiService.getCurrentUser()
                _uiState.update { it.copy(isLoading = false, userProfile = profile) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = ErrorMapper.mapToMeetMindError(e).message ?: "Failed to load profile"
                    ) 
                }
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, error = null, updateSuccess = false) }
            try {
                val updated = userApiService.updateCurrentUser(UserProfileDto(name = name, email = email))
                _uiState.update { it.copy(isUpdating = false, userProfile = updated, updateSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isUpdating = false, 
                        error = ErrorMapper.mapToMeetMindError(e).message ?: "Failed to update profile"
                    ) 
                }
            }
        }
    }
}
