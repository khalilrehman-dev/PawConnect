package com.example.authapp.presentation.vets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.Vet
import com.example.authapp.domain.repository.VetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VetProfileSetupViewModel @Inject constructor(
    private val vetRepository: VetRepository
) : ViewModel() {

    sealed class UiState {
        object Idle    : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun saveVetProfile(
        uid: String,
        displayName: String,
        clinicName: String,
        city: String,
        address: String,
        phoneNumber: String,
        specialization: String,
        yearsOfExperience: Int,
        profileImageUrl: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val vet = Vet(
                uid               = uid,
                displayName       = displayName,
                clinicName        = clinicName,
                city              = city,
                address           = address,
                phoneNumber       = phoneNumber,
                specialization    = specialization,
                yearsOfExperience = yearsOfExperience,
                profileImageUrl   = profileImageUrl,
                isAvailable       = true,
                createdAt         = System.currentTimeMillis()
            )
            vetRepository.saveVetProfile(vet)
                .onSuccess { _uiState.value = UiState.Success }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to save profile") }
        }
    }
}