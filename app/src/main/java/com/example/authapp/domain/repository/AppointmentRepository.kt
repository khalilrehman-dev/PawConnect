package com.example.authapp.domain.repository

import com.example.authapp.model.Appointment

interface AppointmentRepository {

    suspend fun bookAppointment(
        appointment: Appointment
    ): Result<Unit>

    suspend fun getAppointmentsForOwner(
        ownerId: String
    ): Result<List<Appointment>>

    suspend fun getAppointmentsForVet(
        vetId: String
    ): Result<List<Appointment>>

    suspend fun updateAppointmentStatus(
        appointmentId: String,
        vetId: String,
        status: String
    ): Result<Unit>

    suspend fun cancelAppointment(
        appointmentId: String,
        ownerId: String
    ): Result<Unit>
}