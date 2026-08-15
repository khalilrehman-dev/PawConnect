package com.example.authapp.data.repository

import com.example.authapp.model.Vet
import com.example.authapp.domain.repository.VetRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : VetRepository {

    companion object {
        private const val VETS = "vets"
        private const val USERS = "users"
    }

    override suspend fun isVetProfileComplete(
        uid: String
    ): Result<Boolean> = runCatching {

        val document = firestore
            .collection(VETS)
            .document(uid)
            .get()
            .await()

        if (!document.exists()) {
            return@runCatching false
        }

        val displayName =
            document.getString("displayName").orEmpty()

        val clinicName =
            document.getString("clinicName").orEmpty()

        val city =
            document.getString("city").orEmpty()

        val address =
            document.getString("address").orEmpty()

        val phone =
            document.getString("phoneNumber").orEmpty()

        val specialization =
            document.getString("specialization").orEmpty()

        val experience =
            document.getLong("yearsOfExperience")
                ?.toInt() ?: 0

        displayName.isNotBlank() &&
                clinicName.isNotBlank() &&
                city.isNotBlank() &&
                address.isNotBlank() &&
                phone.isNotBlank() &&
                specialization.isNotBlank() &&
                experience > 0
    }

    override suspend fun saveVetProfile(vet: Vet): Result<Unit> =
        runCatching {

            val vetRef =
                firestore.collection(VETS)
                    .document(vet.uid)

            val userRef =
                firestore.collection(USERS)
                    .document(vet.uid)

            val batch = firestore.batch()

            batch.set(
                vetRef,
                vet.toMap()
            )

            batch.update(
                userRef,
                mapOf(
                    "displayName" to vet.displayName,
                    "phoneNumber" to vet.phoneNumber,
                    "profileImageUrl" to vet.profileImageUrl
                )
            )

            batch.commit().await()
        }

    override suspend fun getVetById(uid: String): Result<Vet> = runCatching {
        firestore.collection(VETS)
            .document(uid)
            .get()
            .await()
            .toVet() ?: error("Vet not found")
    }


    override suspend fun getAllVets(): Result<List<Vet>> = runCatching {
        val docs = firestore.collection(VETS)
            .get()
            .await()
            .documents

        // Add this log
        android.util.Log.d("VetRepo", "Docs fetched: ${docs.size}")

        docs.mapNotNull { it.toVet() }
    }


    private fun Vet.toMap() = mapOf(
        "uid"               to uid,
        "displayName"       to displayName,
        "clinicName"        to clinicName,
        "city"              to city,
        "address"           to address,
        "phoneNumber"       to phoneNumber,
        "specialization"    to specialization,
        "yearsOfExperience" to yearsOfExperience,
        "profileImageUrl"   to profileImageUrl,
        "isAvailable"       to isAvailable,
        "createdAt"         to createdAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toVet(): Vet? {
        if (!exists()) return null
        return Vet(
            uid               = getString("uid")             ?: "",
            displayName       = getString("displayName")     ?: "",
            clinicName        = getString("clinicName")      ?: "",
            city              = getString("city")            ?: "",
            address           = getString("address")         ?: "",
            phoneNumber       = getString("phoneNumber")     ?: "",
            specialization    = getString("specialization")  ?: "",
            yearsOfExperience = getLong("yearsOfExperience")?.toInt() ?: 0,
            profileImageUrl   = getString("profileImageUrl") ?: "",
            isAvailable       = getBoolean("isAvailable")   ?: true,
            createdAt         = getLong("createdAt")         ?: 0L
        )
    }

}