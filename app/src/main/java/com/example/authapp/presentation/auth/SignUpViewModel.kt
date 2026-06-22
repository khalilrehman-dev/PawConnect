package com.example.authapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SignUpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun registerWithEmail(name: String, email: String, password: String, role: String) {
        if (!ValidationUtils.isValidName(name)) {
            _uiState.value = SignUpUiState.Error("Enter a valid name")
            return
        }
        if (!ValidationUtils.isValidEmail(email)) {
            _uiState.value = SignUpUiState.Error("Enter a valid email address")
            return
        }
        if (!ValidationUtils.isValidPassword(password)) {
            _uiState.value = SignUpUiState.Error(ValidationUtils.getPasswordError(password))
            return
        }
        if (role.isEmpty()) {
            _uiState.value = SignUpUiState.Error("Please select a role")
            return
        }
        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading
            val result = authRepository.signUp(email, password, name, role)
            if (result.isSuccess) {
                authRepository.sendEmailVerification()
                _events.send(SignUpEvent.GoToEmailOtp(email = email, name = name, role = role))
            } else {
                _uiState.value = SignUpUiState.Error(
                    result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    // Phone signup — just validates then tells Activity to trigger Firebase phone auth
    // (Firebase phone auth must be initiated from an Activity, not a ViewModel)
    fun validatePhoneSignup(name: String, phone: String, role: String) {
        if (!ValidationUtils.isValidName(name)) {
            _uiState.value = SignUpUiState.Error("Enter a valid name")
            return
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            _uiState.value = SignUpUiState.Error("Enter a valid phone number")
            return
        }
        if (role.isEmpty()) {
            _uiState.value = SignUpUiState.Error("Please select a role")
            return
        }
        // Validation passed — tell Activity to start Firebase phone verification
        viewModelScope.launch {
            _events.send(SignUpEvent.StartPhoneVerification(
                phone = ValidationUtils.formatPhoneForFirebase(phone),
                name  = name,
                role  = role
            ))
        }
    }

    fun clearError() { _uiState.value = SignUpUiState.Idle }
}

sealed class SignUpUiState {
    object Idle    : SignUpUiState()
    object Loading : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
}

sealed class SignUpEvent {
    // Email path — go to OTP screen for email link verification
    data class GoToEmailOtp(val email: String, val name: String, val role: String) : SignUpEvent()
    // Phone path — Activity needs to call Firebase verifyPhoneNumber
    data class StartPhoneVerification(val phone: String, val name: String, val role: String) : SignUpEvent()
}