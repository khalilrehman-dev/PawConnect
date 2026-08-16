package com.example.authapp.presentation.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.PetRepository
import com.example.authapp.model.Pet
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


    // =========================================================
    // My Pets list
    // =========================================================

    private val _petsState =
        MutableStateFlow<PetsUiState>(
            PetsUiState.Idle
        )

    val petsState =
        _petsState.asStateFlow()


    // =========================================================
    // Single pet detail
    // =========================================================

    private val _detailState =
        MutableStateFlow<PetDetailUiState>(
            PetDetailUiState.Idle
        )

    val detailState =
        _detailState.asStateFlow()


    // =========================================================
    // Add / Edit / Delete actions
    // =========================================================

    private val _actionState =
        MutableStateFlow<PetActionState>(
            PetActionState.Idle
        )

    val actionState =
        _actionState.asStateFlow()


    // =========================================================
    // One-time events
    // =========================================================

    private val _events =
        Channel<PetEvent>(
            Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()


    /*
     * Selected image is kept temporarily in the ViewModel
     * until add/edit completes.
     */
    private var pendingImageBytes:
            ByteArray? =
        null


    // =========================================================
    // My Pets
    // =========================================================

    fun loadMyPets() {

        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            _petsState.value =
                PetsUiState.Error(
                    "Your session has expired."
                )

            return
        }


        viewModelScope.launch {

            _petsState.value =
                PetsUiState.Loading


            val result =
                petRepository
                    .getPetsByOwner(
                        uid
                    )


            _petsState.value =
                if (
                    result.isSuccess
                ) {

                    val pets =
                        result.getOrThrow()


                    if (
                        pets.isEmpty()
                    ) {

                        PetsUiState.Empty

                    } else {

                        PetsUiState.Success(
                            pets
                        )
                    }

                } else {

                    PetsUiState.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Failed to load pets."
                    )
                }
        }
    }


    // =========================================================
    // Authoritative Pet Detail
    // =========================================================

    fun loadPet(
        petId: String
    ) {

        if (
            petId.isBlank()
        ) {

            _detailState.value =
                PetDetailUiState.Error(
                    "Pet information is missing."
                )

            return
        }


        viewModelScope.launch {

            _detailState.value =
                PetDetailUiState.Loading


            val result =
                petRepository
                    .getPetById(
                        petId
                    )


            _detailState.value =
                if (
                    result.isSuccess
                ) {

                    PetDetailUiState.Success(
                        result.getOrThrow()
                    )

                } else {

                    PetDetailUiState.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Unable to load this pet."
                    )
                }
        }
    }


    /*
     * Edit screen uses a separate authoritative loader.
     *
     * This prevents another user's pet from even being
     * presented as editable if somebody manually launches
     * EditPetActivity with a foreign pet ID.
     */
    fun loadPetForEdit(
        petId: String
    ) {

        val uid =
            authRepository
                .getCurrentUid()


        if (
            uid.isNullOrBlank()
        ) {

            _detailState.value =
                PetDetailUiState.Error(
                    "Your session has expired."
                )

            return
        }


        if (
            petId.isBlank()
        ) {

            _detailState.value =
                PetDetailUiState.Error(
                    "Pet information is missing."
                )

            return
        }


        viewModelScope.launch {

            _detailState.value =
                PetDetailUiState.Loading


            val result =
                petRepository
                    .getPetById(
                        petId
                    )


            if (
                result.isFailure
            ) {

                _detailState.value =
                    PetDetailUiState.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Unable to load this pet."
                    )

                return@launch
            }


            val pet =
                result.getOrThrow()


            if (
                pet.ownerId != uid
            ) {

                _detailState.value =
                    PetDetailUiState.Error(
                        "You can only edit your own pet."
                    )

                return@launch
            }


            _detailState.value =
                PetDetailUiState.Success(
                    pet
                )
        }
    }


    // =========================================================
    // Image
    // =========================================================

    fun setImage(
        bytes: ByteArray
    ) {

        pendingImageBytes =
            bytes
    }


    // =========================================================
    // Add Pet
    // =========================================================

    fun addPet(
        name: String,
        species: String,
        breed: String,
        age: String,
        gender: String,
        description: String
    ) {

        val uid =
            authRepository
                .getCurrentUid()
                ?: run {

                    _actionState.value =
                        PetActionState.Error(
                            "Session expired"
                        )

                    return
                }


        if (
            name.isBlank()
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Enter pet name"
                )

            return
        }


        if (
            species.isBlank() ||
            species ==
            "Select Species"
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Select species"
                )

            return
        }


        if (
            breed.isBlank()
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Enter breed"
                )

            return
        }


        if (
            age.isBlank() ||
            age.toIntOrNull() == null
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Enter valid age"
                )

            return
        }


        if (
            gender.isBlank() ||
            gender ==
            "Select Gender"
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Select gender"
                )

            return
        }


        if (
            pendingImageBytes == null
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Please upload a photo"
                )

            return
        }


        viewModelScope.launch {

            _actionState.value =
                PetActionState.Loading


            val newPet =
                Pet(
                    ownerId =
                        uid,

                    name =
                        name.trim(),

                    species =
                        species,

                    breed =
                        breed.trim(),

                    age =
                        age.toInt(),

                    gender =
                        gender,

                    description =
                        description.trim()
                )


            val addResult =
                petRepository
                    .addPet(
                        newPet
                    )


            if (
                addResult.isFailure
            ) {

                _actionState.value =
                    PetActionState.Error(
                        addResult
                            .exceptionOrNull()
                            ?.message
                            ?: "Failed to add pet"
                    )

                return@launch
            }


            val savedPet =
                addResult.getOrThrow()


            val imageResult =
                petRepository
                    .uploadPetImage(
                        savedPet.id,
                        pendingImageBytes!!
                    )


            if (
                imageResult.isFailure
            ) {

                _actionState.value =
                    PetActionState.Error(
                        "Pet saved but image upload failed"
                    )


                _events.send(
                    PetEvent.NavigateBack
                )


                return@launch
            }


            val imageUrl =
                imageResult.getOrThrow()


            petRepository
                .updatePet(
                    savedPet.copy(
                        imageUrl =
                            imageUrl
                    )
                )


            pendingImageBytes =
                null


            _actionState.value =
                PetActionState.Success


            _events.send(
                PetEvent.NavigateBack
            )
        }
    }


    // =========================================================
    // Edit Pet
    // =========================================================

    fun editPet(
        petId: String,
        name: String,
        species: String,
        breed: String,
        age: String,
        gender: String,
        description: String
    ) {

        val uid =
            authRepository
                .getCurrentUid()
                ?: run {

                    _actionState.value =
                        PetActionState.Error(
                            "Session expired"
                        )

                    return
                }


        if (
            name.isBlank()
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Enter pet name"
                )

            return
        }


        if (
            species.isBlank() ||
            species ==
            "Select Species"
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Select species"
                )

            return
        }


        if (
            breed.isBlank()
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Enter breed"
                )

            return
        }


        if (
            age.isBlank() ||
            age.toIntOrNull() == null
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Enter valid age"
                )

            return
        }


        if (
            gender.isBlank() ||
            gender ==
            "Select Gender"
        ) {

            _actionState.value =
                PetActionState.Error(
                    "Select gender"
                )

            return
        }


        viewModelScope.launch {

            _actionState.value =
                PetActionState.Loading


            /*
             * Refetch authoritative Firestore document.
             */
            val petResult =
                petRepository
                    .getPetById(
                        petId
                    )


            if (
                petResult.isFailure
            ) {

                _actionState.value =
                    PetActionState.Error(
                        "Pet not found"
                    )

                return@launch
            }


            val existingPet =
                petResult.getOrThrow()


            /*
             * Never trust owner information from Intent/UI.
             */
            if (
                existingPet.ownerId != uid
            ) {

                _actionState.value =
                    PetActionState.Error(
                        "You can only edit your own pet"
                    )

                return@launch
            }


            val imageUrl =
                if (
                    pendingImageBytes != null
                ) {

                    val imageResult =
                        petRepository
                            .uploadPetImage(
                                petId,
                                pendingImageBytes!!
                            )


                    if (
                        imageResult.isFailure
                    ) {

                        _actionState.value =
                            PetActionState.Error(
                                "Image upload failed"
                            )

                        return@launch
                    }


                    pendingImageBytes =
                        null


                    imageResult.getOrThrow()

                } else {

                    existingPet.imageUrl
                }


            /*
             * copy() preserves immutable fields such as:
             * ownerId
             * id
             * createdAt
             */
            val updatedPet =
                existingPet.copy(
                    name =
                        name.trim(),

                    species =
                        species,

                    breed =
                        breed.trim(),

                    age =
                        age.toInt(),

                    gender =
                        gender,

                    description =
                        description.trim(),

                    imageUrl =
                        imageUrl
                )


            val result =
                petRepository
                    .updatePet(
                        updatedPet
                    )


            if (
                result.isSuccess
            ) {

                _actionState.value =
                    PetActionState.Success


                _events.send(
                    PetEvent.NavigateBack
                )

            } else {

                _actionState.value =
                    PetActionState.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Failed to update pet"
                    )
            }
        }
    }


    // =========================================================
    // Delete Pet
    // =========================================================

    fun deletePet(
        petId: String
    ) {

        val uid =
            authRepository
                .getCurrentUid()
                ?: run {

                    _actionState.value =
                        PetActionState.Error(
                            "Session expired"
                        )

                    return
                }


        viewModelScope.launch {

            _actionState.value =
                PetActionState.Loading


            val petResult =
                petRepository
                    .getPetById(
                        petId
                    )


            if (
                petResult.isFailure
            ) {

                _actionState.value =
                    PetActionState.Error(
                        "Pet not found"
                    )

                return@launch
            }


            val storedPet =
                petResult.getOrThrow()


            if (
                storedPet.ownerId != uid
            ) {

                _actionState.value =
                    PetActionState.Error(
                        "You can only delete your own pet"
                    )

                return@launch
            }


            val result =
                petRepository
                    .deletePet(
                        storedPet.id,
                        storedPet.imageUrl
                    )


            if (
                result.isSuccess
            ) {

                _actionState.value =
                    PetActionState.Success


                loadMyPets()

            } else {

                _actionState.value =
                    PetActionState.Error(
                        result
                            .exceptionOrNull()
                            ?.message
                            ?: "Failed to delete pet"
                    )
            }
        }
    }


    fun clearActionState() {

        _actionState.value =
            PetActionState.Idle
    }
}


// =============================================================
// List State
// =============================================================

sealed class PetsUiState {

    object Idle :
        PetsUiState()


    object Loading :
        PetsUiState()


    object Empty :
        PetsUiState()


    data class Success(
        val pets: List<Pet>
    ) : PetsUiState()


    data class Error(
        val message: String
    ) : PetsUiState()
}


// =============================================================
// Detail State
// =============================================================

sealed class PetDetailUiState {

    object Idle :
        PetDetailUiState()


    object Loading :
        PetDetailUiState()


    data class Success(
        val pet: Pet
    ) : PetDetailUiState()


    data class Error(
        val message: String
    ) : PetDetailUiState()
}


// =============================================================
// Action State
// =============================================================

sealed class PetActionState {

    object Idle :
        PetActionState()


    object Loading :
        PetActionState()


    object Success :
        PetActionState()


    data class Error(
        val message: String
    ) : PetActionState()
}


// =============================================================
// Events
// =============================================================

sealed class PetEvent {

    object NavigateBack :
        PetEvent()
}