package com.example.authapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.User
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loginWithEmail(email: String, password: String) {
        if (!ValidationUtils.isValidEmail(email)) {
            _uiState.value = LoginUiState.Error("Enter a valid email address")
            return
        }
        if (password.isEmpty()) {
            _uiState.value = LoginUiState.Error("Enter your password")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                if (!user.isEmailVerified) {
                    authRepository.logout()
                    _uiState.value = LoginUiState.Error("Please verify your email first")
                } else {
                    val dest = if (user.role.isEmpty()) "role_selection" else "dashboard"
                    _events.send(LoginEvent.NavigateTo(dest))
                }
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun clearError() { _uiState.value = LoginUiState.Idle }
}

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

sealed class LoginEvent {
    data class NavigateTo(val destination: String) : LoginEvent()
}