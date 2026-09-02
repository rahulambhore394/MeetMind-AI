package com.developer_rahul.meetmind_ai.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer_rahul.meetmind_ai.core.network.model.NetworkResult
import com.developer_rahul.meetmind_ai.feature.auth.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun login() {
        if (!validateLogin()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(_uiState.value.email, _uiState.value.password)
            _uiState.update { state ->
                when (result) {
                    is NetworkResult.Success -> state.copy(isLoading = false, isSuccess = true)
                    is NetworkResult.Error -> state.copy(isLoading = false, error = result.error.message ?: "Login failed")
                    else -> state.copy(isLoading = false)
                }
            }
        }
    }

    fun register() {
        if (!validateRegister()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val name = _uiState.value.name
            val email = _uiState.value.email
            val password = _uiState.value.password

            when (val result = authRepository.register(name, email, password)) {
                is NetworkResult.Success -> {
                    // Auto-login after successful registration to save token
                    when (val loginResult = authRepository.login(email, password)) {
                        is NetworkResult.Success -> {
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        }
                        is NetworkResult.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = loginResult.error.message ?: "Registration succeeded, but auto-login failed. Please sign in.") }
                        }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message ?: "Registration failed") }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { AuthUiState() }
    }

    private fun validateLogin(): Boolean {
        var isValid = true
        if (_uiState.value.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        }
        if (_uiState.value.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (_uiState.value.password.length < 8) {
            _uiState.update { it.copy(passwordError = "Password must be at least 8 characters") }
            isValid = false
        }
        return isValid
    }

    private fun validateRegister(): Boolean {
        var isValid = validateLogin()
        if (_uiState.value.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            isValid = false
        }
        return isValid
    }
}

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
