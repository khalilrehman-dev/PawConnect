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
    }

    override suspend fun saveVetProfile(vet: Vet): Result<Unit> = runCatching {
        firestore.collection(VETS)
            .document(vet.uid)
            .set(vet.toMap())
            .await()
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