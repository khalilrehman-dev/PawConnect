package com.example.authapp.domain.repository

import com.example.authapp.model.Appointment

interface AppointmentRepository {

    /** Pet owner books an appointment */
    suspend fun bookAppointment(appointment: Appointment): Result<Unit>

    /** Get all appointments for a pet owner */
    suspend fun getAppointmentsForOwner(ownerId: String): Result<List<Appointment>>

    /** Get all appointments for a vet */
    suspend fun getAppointmentsForVet(vetId: String): Result<List<Appointment>>

    /** Vet accepts or rejects an appointment */
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit>
}