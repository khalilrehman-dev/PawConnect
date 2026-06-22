package com.example.authapp.data.repository

import com.example.authapp.model.Appointment
import com.example.authapp.domain.repository.AppointmentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AppointmentRepository {

    companion object {
        private const val APPOINTMENTS = "appointments"
    }

    override suspend fun bookAppointment(appointment: Appointment): Result<Unit> = runCatching {
        val docRef = firestore.collection(APPOINTMENTS).document()
        val withId = appointment.copy(
            id        = docRef.id,
            createdAt = System.currentTimeMillis()
        )
        docRef.set(withId.toMap()).await()
    }

    override suspend fun getAppointmentsForOwner(ownerId: String): Result<List<Appointment>> = runCatching {
        firestore.collection(APPOINTMENTS)
            .whereEqualTo("petOwnerId", ownerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toAppointment() }
    }

    override suspend fun getAppointmentsForVet(vetId: String): Result<List<Appointment>> = runCatching {
        firestore.collection(APPOINTMENTS)
            .whereEqualTo("vetId", vetId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toAppointment() }
    }

    override suspend fun updateAppointmentStatus(
        appointmentId: String,
        status: String
    ): Result<Unit> = runCatching {
        firestore.collection(APPOINTMENTS)
            .document(appointmentId)
            .update("status", status)
            .await()
    }

    private fun Appointment.toMap() = mapOf(
        "id"          to id,
        "petOwnerId"  to petOwnerId,
        "vetId"       to vetId,
        "vetName"     to vetName,
        "clinicName"  to clinicName,
        "petId"       to petId,
        "petName"     to petName,
        "date"        to date,
        "time"        to time,
        "note"        to note,
        "status"      to status,
        "createdAt"   to createdAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toAppointment(): Appointment? {
        if (!exists()) return null
        return Appointment(
            id         = getString("id")         ?: "",
            petOwnerId = getString("petOwnerId") ?: "",
            vetId      = getString("vetId")      ?: "",
            vetName    = getString("vetName")    ?: "",
            clinicName = getString("clinicName") ?: "",
            petId      = getString("petId")      ?: "",
            petName    = getString("petName")    ?: "",
            date       = getString("date")       ?: "",
            time       = getString("time")       ?: "",
            note       = getString("note")       ?: "",
            status     = getString("status")     ?: "pending",
            createdAt  = getLong("createdAt")    ?: 0L
        )
    }
}