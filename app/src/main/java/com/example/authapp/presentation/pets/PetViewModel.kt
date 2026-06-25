package com.example.authapp.presentation.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.Pet
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _petsState = MutableStateFlow<PetsUiState>(PetsUiState.Idle)
    val petsState = _petsState.asStateFlow()

    private val _actionState = MutableStateFlow<PetActionState>(PetActionState.Idle)
    val actionState = _actionState.asStateFlow()

    private val _events = Channel<PetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Temporarily hold image bytes until pet is saved
    private var pendingImageBytes: ByteArray? = null

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadMyPets() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _petsState.value = PetsUiState.Loading
            val result = petRepository.getPetsByOwner(uid)
            _petsState.value = if (result.isSuccess) {
                val pets = result.getOrThrow()
                if (pets.isEmpty()) PetsUiState.Empty
                else PetsUiState.Success(pets)
            } else {
                PetsUiState.Error(result.exceptionOrNull()?.message ?: "Failed to load pets")
            }
        }
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    fun setImage(bytes: ByteArray) {
        pendingImageBytes = bytes
    }

    fun addPet(name: String, species: String, breed: String, age: String, gender: String, description: String) {
        val uid = authRepository.getCurrentUid() ?: run {
            _actionState.value = PetActionState.Error("Session expired")
            return
        }

        // Validate
        if (name.isBlank())    { _actionState.value = PetActionState.Error("Enter pet name"); return }
        if (species.isBlank()) { _actionState.value = PetActionState.Error("Select species"); return }
        if (breed.isBlank())   { _actionState.value = PetActionState.Error("Enter breed"); return }
        if (age.isBlank() || age.toIntOrNull() == null) { _actionState.value = PetActionState.Error("Enter valid age"); return }
        if (gender.isBlank())  { _actionState.value = PetActionState.Error("Select gender"); return }
        if (pendingImageBytes == null) { _actionState.value = PetActionState.Error("Please upload a photo"); return }

        viewModelScope.launch {
            _actionState.value = PetActionState.Loading

            // 1. Create pet doc to get ID
            val newPet = Pet(
                ownerId = uid,
                name = name.trim(),
                species = species,
                breed = breed.trim(),
                age = age.toInt(),
                gender = gender,
                description = description.trim()
            )
            val addResult = petRepository.addPet(newPet)
            if (addResult.isFailure) {
                _actionState.value = PetActionState.Error(addResult.exceptionOrNull()?.message ?: "Failed to add pet")
                return@launch
            }

            val savedPet = addResult.getOrThrow()

            // 2. Upload image using the generated petId
            val imageResult = petRepository.uploadPetImage(savedPet.id, pendingImageBytes!!)
            if (imageResult.isFailure) {
                _actionState.value = PetActionState.Error("Pet saved but image upload failed")
                _events.send(PetEvent.NavigateBack)
                return@launch
            }

            // 3. Update pet doc with image URL
            val imageUrl = imageResult.getOrThrow()
            petRepository.updatePet(savedPet.copy(imageUrl = imageUrl))

            pendingImageBytes = null
            _actionState.value = PetActionState.Success
            _events.send(PetEvent.NavigateBack)
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deletePet(pet: Pet) {
        viewModelScope.launch {
            _actionState.value = PetActionState.Loading
            val result = petRepository.deletePet(pet.id, pet.imageUrl)
            if (result.isSuccess) {
                _actionState.value = PetActionState.Success
                loadMyPets() // refresh list
            } else {
                _actionState.value = PetActionState.Error(result.exceptionOrNull()?.message ?: "Failed to delete pet")
            }
        }
    }

    fun editPet(
        petId: String,
        ownerId: String,
        name: String,
        species: String,
        breed: String,
        age: String,
        gender: String,
        description: String,
        existingImageUrl: String
    ) {
        if (name.isBlank())    { _actionState.value = PetActionState.Error("Enter pet name"); return }
        if (species.isBlank()) { _actionState.value = PetActionState.Error("Select species"); return }
        if (breed.isBlank())   { _actionState.value = PetActionState.Error("Enter breed"); return }
        if (age.isBlank() || age.toIntOrNull() == null) { _actionState.value = PetActionState.Error("Enter valid age"); return }
        if (gender.isBlank())  { _actionState.value = PetActionState.Error("Select gender"); return }

        viewModelScope.launch {
            _actionState.value = PetActionState.Loading

            // If new image selected upload it, otherwise keep existing
            val imageUrl = if (pendingImageBytes != null) {
                val imageResult = petRepository.uploadPetImage(petId, pendingImageBytes!!)
                if (imageResult.isFailure) {
                    _actionState.value = PetActionState.Error("Image upload failed")
                    return@launch
                }
                pendingImageBytes = null
                imageResult.getOrThrow()
            } else {
                existingImageUrl
            }

            val updatedPet = Pet(
                id          = petId,
                ownerId     = ownerId,
                name        = name.trim(),
                species     = species,
                breed       = breed.trim(),
                age         = age.toInt(),
                gender      = gender,
                description = description.trim(),
                imageUrl    = imageUrl
            )

            val result = petRepository.updatePet(updatedPet)
            if (result.isSuccess) {
                _actionState.value = PetActionState.Success
                _events.send(PetEvent.NavigateBack)
            } else {
                _actionState.value = PetActionState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to update pet"
                )
            }
        }
    }

    fun clearActionState() { _actionState.value = PetActionState.Idle }
}

// ── State & Event models ──────────────────────────────────────────────────────

sealed class PetsUiState {
    object Idle    : PetsUiState()
    object Loading : PetsUiState()
    object Empty   : PetsUiState()
    data class Success(val pets: List<Pet>) : PetsUiState()
    data class Error(val message: String)   : PetsUiState()
}

sealed class PetActionState {
    object Idle    : PetActionState()
    object Loading : PetActionState()
    object Success : PetActionState()
    data class Error(val message: String) : PetActionState()
}

sealed class PetEvent {
    object NavigateBack : PetEvent()
}
