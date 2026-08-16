package com.example.authapp.data.repository

import com.example.authapp.model.Appointment
import com.example.authapp.domain.repository.AppointmentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestoreException

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AppointmentRepository {

    companion object {
        private const val APPOINTMENTS = "appointments"
        private const val APPOINTMENT_SLOTS = "appointmentSlots"
    }

    override suspend fun cancelAppointment(
        appointmentId: String,
        ownerId: String
    ): Result<Unit> = runCatching {

        val appointmentRef =
            firestore.collection(APPOINTMENTS)
                .document(appointmentId)

        firestore.runTransaction { transaction ->

            val snapshot =
                transaction.get(appointmentRef)

            if (!snapshot.exists()) {
                throw FirebaseFirestoreException(
                    "Appointment not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }

            val storedOwnerId =
                snapshot.getString("petOwnerId").orEmpty()

            if (storedOwnerId != ownerId) {
                throw FirebaseFirestoreException(
                    "You cannot cancel this appointment",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }

            val currentStatus =
                snapshot.getString("status")
                    ?: "pending"

            if (
                currentStatus != "pending" &&
                currentStatus != "accepted"
            ) {
                throw FirebaseFirestoreException(
                    "This appointment cannot be cancelled",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }

            transaction.update(
                appointmentRef,
                "status",
                "cancelled"
            )

            val vetId =
                snapshot.getString("vetId").orEmpty()

            val scheduledAt =
                snapshot.getLong("scheduledAt")
                    ?: 0L

            if (
                vetId.isNotBlank() &&
                scheduledAt > 0L
            ) {

                val slotRef =
                    firestore.collection(
                        APPOINTMENT_SLOTS
                    ).document(
                        createSlotId(
                            vetId,
                            scheduledAt
                        )
                    )

                transaction.delete(slotRef)
            }

            true
        }.await()
    }

    override suspend fun bookAppointment(
        appointment: Appointment
    ): Result<Unit> = runCatching {

        if (appointment.petOwnerId.isBlank()) {
            error("Owner session is missing")
        }

        if (appointment.vetId.isBlank()) {
            error("Vet not found")
        }

        if (appointment.petId.isBlank()) {
            error("Select a pet")
        }

        if (appointment.scheduledAt <= System.currentTimeMillis()) {
            error("Please select a future appointment time")
        }

        val appointmentRef =
            firestore.collection(APPOINTMENTS)
                .document()

        val slotRef =
            firestore.collection(APPOINTMENT_SLOTS)
                .document(
                    createSlotId(
                        appointment.vetId,
                        appointment.scheduledAt
                    )
                )

        val finalAppointment =
            appointment.copy(
                id = appointmentRef.id,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )

        firestore.runTransaction { transaction ->

            val existingSlot =
                transaction.get(slotRef)

            if (existingSlot.exists()) {
                throw FirebaseFirestoreException(
                    "This appointment time is already booked",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }

            transaction.set(
                appointmentRef,
                finalAppointment.toMap()
            )

            transaction.set(
                slotRef,
                mapOf(
                    "appointmentId" to appointmentRef.id,
                    "vetId" to appointment.vetId,
                    "scheduledAt" to appointment.scheduledAt,
                    "createdAt" to System.currentTimeMillis()
                )
            )

            true
        }.await()
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
        vetId: String,
        status: String
    ): Result<Unit> = runCatching {

        if (
            status != "accepted" &&
            status != "rejected"
        ) {
            error("Invalid appointment status")
        }

        val appointmentRef =
            firestore.collection(APPOINTMENTS)
                .document(appointmentId)

        firestore.runTransaction { transaction ->

            val snapshot =
                transaction.get(appointmentRef)

            if (!snapshot.exists()) {
                throw FirebaseFirestoreException(
                    "Appointment not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }

            val assignedVetId =
                snapshot.getString("vetId").orEmpty()

            if (assignedVetId != vetId) {
                throw FirebaseFirestoreException(
                    "You cannot update this appointment",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }

            val currentStatus =
                snapshot.getString("status")
                    ?: "pending"

            if (currentStatus != "pending") {
                throw FirebaseFirestoreException(
                    "Appointment has already been processed",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }

            transaction.update(
                appointmentRef,
                "status",
                status
            )

            // Rejected appointment releases the slot.
            if (status == "rejected") {

                val scheduledAt =
                    snapshot.getLong("scheduledAt")
                        ?: 0L

                if (scheduledAt > 0L) {

                    val slotRef =
                        firestore.collection(
                            APPOINTMENT_SLOTS
                        ).document(
                            createSlotId(
                                vetId,
                                scheduledAt
                            )
                        )

                    transaction.delete(slotRef)
                }
            }

            true
        }.await()
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
        "scheduledAt" to scheduledAt,
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
            scheduledAt = getLong("scheduledAt") ?: 0L,
            note       = getString("note")       ?: "",
            status     = getString("status")     ?: "pending",
            createdAt  = getLong("createdAt")    ?: 0L
        )
    }
    private fun createSlotId(
        vetId: String,
        scheduledAt: Long
    ): String {
        return "${vetId}_${scheduledAt}"
    }
}