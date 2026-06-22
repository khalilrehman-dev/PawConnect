package com.example.authapp.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.model.Appointment
import com.example.authapp.model.Pet
import com.example.authapp.domain.repository.AppointmentRepository
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
class AppointmentViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val petRepository: PetRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── States ────────────────────────────────────────────────────────────────

    private val _appointmentsState = MutableStateFlow<AppointmentListState>(AppointmentListState.Idle)
    val appointmentsState = _appointmentsState.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState = _bookingState.asStateFlow()

    private val _petsState = MutableStateFlow<List<Pet>>(emptyList())
    val petsState = _petsState.asStateFlow()

    private val _events = Channel<AppointmentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Load owner's pets for booking screen ──────────────────────────────────

    fun loadMyPets() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            val result = petRepository.getPetsByOwner(uid)
            if (result.isSuccess) {
                _petsState.value = result.getOrThrow()
            }
        }
    }

    // ── Book appointment ──────────────────────────────────────────────────────

    fun bookAppointment(
        vetId: String,
        vetName: String,
        clinicName: String,
        selectedPet: Pet,
        date: String,
        time: String,
        note: String
    ) {
        val ownerId = authRepository.getCurrentUid() ?: run {
            _bookingState.value = BookingState.Error("Session expired")
            return
        }

        if (date.isBlank()) { _bookingState.value = BookingState.Error("Select a date"); return }
        if (time.isBlank()) { _bookingState.value = BookingState.Error("Select a time"); return }

        viewModelScope.launch {
            _bookingState.value = BookingState.Loading

            val appointment = Appointment(
                petOwnerId = ownerId,
                vetId      = vetId,
                vetName    = vetName,
                clinicName = clinicName,
                petId      = selectedPet.id,
                petName    = selectedPet.name,
                date       = date,
                time       = time,
                note       = note.trim(),
                status     = "pending"
            )

            val result = appointmentRepository.bookAppointment(appointment)
            if (result.isSuccess) {
                _bookingState.value = BookingState.Success
                _events.send(AppointmentEvent.BookingSuccess)
            } else {
                _bookingState.value = BookingState.Error(
                    result.exceptionOrNull()?.message ?: "Booking failed"
                )
            }
        }
    }

    // ── Load appointments for pet owner ───────────────────────────────────────

    fun loadOwnerAppointments() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _appointmentsState.value = AppointmentListState.Loading
            val result = appointmentRepository.getAppointmentsForOwner(uid)
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _appointmentsState.value = if (list.isEmpty()) AppointmentListState.Empty
                else AppointmentListState.Success(list)
            } else {
                _appointmentsState.value = AppointmentListState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load"
                )
            }
        }
    }

    // ── Load appointments for vet ─────────────────────────────────────────────

    fun loadVetAppointments() {
        val uid = authRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _appointmentsState.value = AppointmentListState.Loading
            val result = appointmentRepository.getAppointmentsForVet(uid)
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _appointmentsState.value = if (list.isEmpty()) AppointmentListState.Empty
                else AppointmentListState.Success(list)
            } else {
                _appointmentsState.value = AppointmentListState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load"
                )
            }
        }
    }

    // ── Vet accepts or rejects ────────────────────────────────────────────────

    fun updateStatus(appointmentId: String, status: String) {
        viewModelScope.launch {
            val result = appointmentRepository.updateAppointmentStatus(appointmentId, status)
            if (result.isSuccess) {
                loadVetAppointments() // refresh list
            } else {
                _events.send(
                    AppointmentEvent.Error(result.exceptionOrNull()?.message ?: "Failed to update")
                )
            }
        }
    }
}

// ── State models ──────────────────────────────────────────────────────────────

sealed class AppointmentListState {
    object Idle    : AppointmentListState()
    object Loading : AppointmentListState()
    object Empty   : AppointmentListState()
    data class Success(val appointments: List<Appointment>) : AppointmentListState()
    data class Error(val message: String) : AppointmentListState()
}

sealed class BookingState {
    object Idle    : BookingState()
    object Loading : BookingState()
    object Success : BookingState()
    data class Error(val message: String) : BookingState()
}

sealed class AppointmentEvent {
    object BookingSuccess : AppointmentEvent()
    data class Error(val message: String) : AppointmentEvent()
}