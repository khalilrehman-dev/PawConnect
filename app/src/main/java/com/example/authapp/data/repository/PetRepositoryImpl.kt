package com.example.authapp.data.repository

import com.example.authapp.data.remote.CloudinaryUploader
import com.example.authapp.model.Pet
import com.example.authapp.domain.repository.PetRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader   // ← Cloudinary, not Firebase Storage
) : PetRepository {

    companion object {
        private const val PETS = "pets"
    }

    override suspend fun getPetsByOwner(ownerId: String): Result<List<Pet>> = runCatching {
        firestore.collection(PETS)
            .whereEqualTo("ownerId", ownerId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toPet() }
    }

    override suspend fun getPetById(petId: String): Result<Pet> = runCatching {
        firestore.collection(PETS)
            .document(petId)
            .get()
            .await()
            .toPet() ?: error("Pet not found")
    }

    override suspend fun addPet(pet: Pet): Result<Pet> = runCatching {
        val docRef    = firestore.collection(PETS).document()
        val petWithId = pet.copy(id = docRef.id, createdAt = System.currentTimeMillis())
        docRef.set(petWithId.toMap()).await()
        petWithId
    }

    override suspend fun updatePet(pet: Pet): Result<Unit> = runCatching {
        firestore.collection(PETS)
            .document(pet.id)
            .update(pet.toMap())
            .await()
    }

    override suspend fun deletePet(petId: String, imageUrl: String): Result<Unit> = runCatching {
        // Only delete Firestore doc — Cloudinary images stay (free tier, no delete needed)
        firestore.collection(PETS).document(petId).delete().await()
    }

    override suspend fun uploadPetImage(petId: String, imageBytes: ByteArray): Result<String> {
        // Upload to Cloudinary — returns secure HTTPS URL
        return cloudinaryUploader.uploadImage(
            imageBytes = imageBytes,
            folder     = "pawconnect/pets/$petId"
        )
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Pet.toMap() = mapOf(
        "id"          to id,
        "ownerId"     to ownerId,
        "name"        to name,
        "species"     to species,
        "breed"       to breed,
        "age"         to age,
        "gender"      to gender,
        "description" to description,
        "imageUrl"    to imageUrl,
        "createdAt"   to createdAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toPet(): Pet? {
        if (!exists()) return null
        return Pet(
            id          = getString("id")          ?: "",
            ownerId     = getString("ownerId")     ?: "",
            name        = getString("name")        ?: "",
            species     = getString("species")     ?: "",
            breed       = getString("breed")       ?: "",
            age         = getLong("age")?.toInt()  ?: 0,
            gender      = getString("gender")      ?: "",
            description = getString("description") ?: "",
            imageUrl    = getString("imageUrl")    ?: "",
            createdAt   = getLong("createdAt")     ?: 0L
        )
    }
    override suspend fun getAllPets(): Result<List<Pet>> = runCatching {
        firestore.collection(PETS)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toPet() }
    }



}
