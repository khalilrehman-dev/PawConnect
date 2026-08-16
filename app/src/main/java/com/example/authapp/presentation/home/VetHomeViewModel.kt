package com.example.authapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.AppointmentRepository
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.ChatRepository
import com.example.authapp.domain.repository.VetRepository
import com.example.authapp.model.Appointment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class VetHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vetRepository: VetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<VetHomeUiState>(
            VetHomeUiState.Loading
        )

    val uiState =
        _uiState.asStateFlow()


    private var unreadJob: Job? = null


    fun loadHome() {

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            _uiState.value =
                VetHomeUiState.Error(
                    "Your session has expired. Please sign in again."
                )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                VetHomeUiState.Loading


            // ─────────────────────────────────────────
            // Basic user profile
            // ─────────────────────────────────────────

            val userResult =
                authRepository
                    .getUserFromFirestore(uid)

            if (userResult.isFailure) {

                _uiState.value =
                    VetHomeUiState.Error(
                        "Unable to load your profile."
                    )

                return@launch
            }

            val user =
                userResult.getOrThrow()


            // ─────────────────────────────────────────
            // Vet appointments
            // ─────────────────────────────────────────

            val appointmentResult =
                appointmentRepository
                    .getAppointmentsForVet(uid)

            if (appointmentResult.isFailure) {

                _uiState.value =
                    VetHomeUiState.Error(
                        "Unable to load your appointments."
                    )

                return@launch
            }


            val appointments =
                appointmentResult.getOrThrow()


            val pendingRequestCount =
                appointments.count { appointment ->

                    appointment.status
                        .equals(
                            "pending",
                            ignoreCase = true
                        )
                }


            /*
             * A patient is counted once even if
             * the same pet has multiple accepted appointments.
             */
            val patientCount =
                appointments
                    .filter { appointment ->

                        appointment.status
                            .equals(
                                "accepted",
                                ignoreCase = true
                            )
                    }
                    .map { appointment ->
                        appointment.petId
                    }
                    .filter { petId ->
                        petId.isNotBlank()
                    }
                    .distinct()
                    .size


            val now =
                System.currentTimeMillis()


            val upcomingAppointment =
                appointments
                    .filter { appointment ->

                        appointment.scheduledAt > now &&

                                (
                                        appointment.status
                                            .equals(
                                                "pending",
                                                ignoreCase = true
                                            ) ||

                                                appointment.status
                                                    .equals(
                                                        "accepted",
                                                        ignoreCase = true
                                                    )
                                        )
                    }
                    .minByOrNull { appointment ->
                        appointment.scheduledAt
                    }


            // ─────────────────────────────────────────
            // Professional profile
            // ─────────────────────────────────────────

            val profileResult =
                vetRepository
                    .isVetProfileComplete(uid)

            /*
             * null means the check failed.
             * We don't incorrectly show an
             * incomplete-profile warning on
             * a temporary network failure.
             */
            val isProfileComplete =
                profileResult.getOrNull()


            val displayName =
                user.displayName
                    .trim()
                    .ifBlank {
                        "Veterinarian"
                    }


            _uiState.value =
                VetHomeUiState.Success(
                    displayName = displayName,
                    profileImageUrl = user.profileImageUrl,
                    pendingRequestCount = pendingRequestCount,
                    patientCount = patientCount,
                    unreadCount = 0,
                    upcomingAppointment = upcomingAppointment,
                    isProfileComplete = isProfileComplete
                )


            observeUnreadChats(uid)
        }
    }


    private fun observeUnreadChats(
        uid: String
    ) {

        unreadJob?.cancel()


        unreadJob =
            viewModelScope.launch {

                chatRepository
                    .getInbox(uid)
                    .catch {
                        /*
                         * Home remains usable if inbox
                         * temporarily fails to load.
                         */
                    }
                    .collect { chats ->

                        val unreadCount =
                            chats.count { chat ->
                                uid in chat.unreadBy
                            }


                        val currentState =
                            _uiState.value


                        if (
                            currentState
                                    is VetHomeUiState.Success
                        ) {

                            _uiState.value =
                                currentState.copy(
                                    unreadCount =
                                        unreadCount
                                )
                        }
                    }
            }
    }
}


sealed class VetHomeUiState {

    object Loading :
        VetHomeUiState()


    data class Success(
        val displayName: String,
        val profileImageUrl: String,
        val pendingRequestCount: Int,
        val patientCount: Int,
        val unreadCount: Int,
        val upcomingAppointment: Appointment?,
        val isProfileComplete: Boolean?
    ) : VetHomeUiState()


    data class Error(
        val message: String
    ) : VetHomeUiState()
}