package com.example.authapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.AppointmentRepository
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.ChatRepository
import com.example.authapp.domain.repository.PetRepository
import com.example.authapp.model.Appointment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OwnerHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val appointmentRepository: AppointmentRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<OwnerHomeUiState>(
            OwnerHomeUiState.Loading
        )

    val uiState =
        _uiState.asStateFlow()


    private var unreadJob: Job? = null


    fun loadHome() {

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            _uiState.value =
                OwnerHomeUiState.Error(
                    "Your session has expired. Please sign in again."
                )

            return
        }


        viewModelScope.launch {

            _uiState.value =
                OwnerHomeUiState.Loading


            // ─────────────────────────────────────────
            // User
            // ─────────────────────────────────────────

            val userResult =
                authRepository
                    .getUserFromFirestore(uid)

            if (userResult.isFailure) {

                _uiState.value =
                    OwnerHomeUiState.Error(
                        "Unable to load your profile."
                    )

                return@launch
            }

            val user =
                userResult.getOrThrow()


            // ─────────────────────────────────────────
            // Pets
            // ─────────────────────────────────────────

            val petsResult =
                petRepository
                    .getPetsByOwner(uid)

            if (petsResult.isFailure) {

                _uiState.value =
                    OwnerHomeUiState.Error(
                        "Unable to load your pets."
                    )

                return@launch
            }

            val pets =
                petsResult.getOrThrow()


            // ─────────────────────────────────────────
            // Appointments
            // ─────────────────────────────────────────

            val appointmentsResult =
                appointmentRepository
                    .getAppointmentsForOwner(uid)

            if (appointmentsResult.isFailure) {

                _uiState.value =
                    OwnerHomeUiState.Error(
                        "Unable to load your appointments."
                    )

                return@launch
            }

            val appointments =
                appointmentsResult.getOrThrow()


            val now =
                System.currentTimeMillis()


            val upcomingAppointment =
                appointments
                    .filter { appointment ->

                        appointment.scheduledAt > now &&

                                (
                                        appointment.status == "pending" ||
                                                appointment.status == "accepted"
                                        )
                    }
                    .minByOrNull { appointment ->
                        appointment.scheduledAt
                    }


            val displayName =
                user.displayName
                    .trim()
                    .ifBlank {
                        "Pet Owner"
                    }


            _uiState.value =
                OwnerHomeUiState.Success(
                    displayName = displayName,
                    profileImageUrl = user.profileImageUrl,
                    petCount = pets.size,
                    upcomingAppointment = upcomingAppointment,
                    unreadCount = 0
                )


            observeUnreadMessages(uid)
        }
    }


    private fun observeUnreadMessages(
        uid: String
    ) {

        unreadJob?.cancel()

        unreadJob =
            viewModelScope.launch {

                chatRepository
                    .getInbox(uid)
                    .catch {
                        /*
                         * Home is still usable if
                         * inbox loading temporarily fails.
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
                                    is OwnerHomeUiState.Success
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


sealed class OwnerHomeUiState {

    object Loading :
        OwnerHomeUiState()


    data class Success(
        val displayName: String,
        val profileImageUrl: String,
        val petCount: Int,
        val upcomingAppointment: Appointment?,
        val unreadCount: Int
    ) : OwnerHomeUiState()


    data class Error(
        val message: String
    ) : OwnerHomeUiState()
}