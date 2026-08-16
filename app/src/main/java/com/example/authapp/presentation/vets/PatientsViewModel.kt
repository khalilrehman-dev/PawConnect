package com.example.authapp.presentation.vets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.AppointmentRepository
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.PetRepository
import com.example.authapp.model.Appointment
import com.example.authapp.model.Pet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PatientsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<PatientsUiState>(
            PatientsUiState.Loading
        )

    val uiState =
        _uiState.asStateFlow()


    fun loadPatients() {

        val vetUid =
            authRepository.getCurrentUid()

        if (vetUid.isNullOrBlank()) {

            _uiState.value =
                PatientsUiState.Error(
                    "Your session has expired. Please sign in again."
                )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                PatientsUiState.Loading


            val appointmentsResult =
                appointmentRepository
                    .getAppointmentsForVet(
                        vetUid
                    )


            if (appointmentsResult.isFailure) {

                _uiState.value =
                    PatientsUiState.Error(
                        appointmentsResult
                            .exceptionOrNull()
                            ?.message
                            ?: "Unable to load patients."
                    )

                return@launch
            }


            /*
             * A patient should appear only after
             * an appointment has been accepted.
             */
            val acceptedAppointments =
                appointmentsResult
                    .getOrThrow()
                    .filter { appointment ->

                        appointment.status
                            .equals(
                                "accepted",
                                ignoreCase = true
                            ) &&

                                appointment.petId
                                    .isNotBlank()
                    }


            if (acceptedAppointments.isEmpty()) {

                _uiState.value =
                    PatientsUiState.Empty

                return@launch
            }


            /*
             * One pet may have several accepted
             * appointments.
             *
             * Keep only one patient card per pet.
             * The appointment with the latest
             * scheduled time is retained.
             */
            val uniquePatientAppointments =
                acceptedAppointments
                    .sortedByDescending { appointment ->

                        appointment.scheduledAt
                    }
                    .distinctBy { appointment ->

                        appointment.petId
                    }


            val patientItems =
                mutableListOf<PatientUiItem>()


            for (
            appointment
            in uniquePatientAppointments
            ) {

                val petResult =
                    petRepository
                        .getPetById(
                            appointment.petId
                        )


                /*
                 * If the pet document was removed,
                 * don't show a broken patient card.
                 */
                val pet =
                    petResult.getOrNull()
                        ?: continue


                val ownerUid =
                    pet.ownerId
                        .ifBlank {

                            appointment.petOwnerId
                        }


                val ownerName =
                    if (ownerUid.isBlank()) {

                        "Pet Owner"

                    } else {

                        authRepository
                            .getUserFromFirestore(
                                ownerUid
                            )
                            .getOrNull()
                            ?.displayName
                            ?.trim()
                            .orEmpty()
                            .ifBlank {

                                "Pet Owner"
                            }
                    }


                patientItems.add(
                    PatientUiItem(
                        pet = pet,
                        ownerName = ownerName,
                        appointment = appointment
                    )
                )
            }


            if (patientItems.isEmpty()) {

                _uiState.value =
                    PatientsUiState.Empty

            } else {

                /*
                 * Alphabetical patient list is easier
                 * to scan than appointment creation order.
                 */
                _uiState.value =
                    PatientsUiState.Success(
                        patients =
                            patientItems
                                .sortedBy { item ->

                                    item.pet.name
                                        .lowercase()
                                }
                    )
            }
        }
    }
}


sealed class PatientsUiState {

    object Loading :
        PatientsUiState()


    data class Success(
        val patients: List<PatientUiItem>
    ) : PatientsUiState()


    object Empty :
        PatientsUiState()


    data class Error(
        val message: String
    ) : PatientsUiState()
}


data class PatientUiItem(
    val pet: Pet,
    val ownerName: String,
    val appointment: Appointment
)