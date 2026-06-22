package com.example.authapp.domain.repository

import com.example.authapp.model.Pet

interface PetRepository {

    /** Get all pets owned by a specific user */
    suspend fun getPetsByOwner(ownerId: String): Result<List<Pet>>

    /** Get a single pet by its Firestore document ID */
    suspend fun getPetById(petId: String): Result<Pet>

    /** Add a new pet — returns the pet with its generated ID */
    suspend fun addPet(pet: Pet): Result<Pet>

    /** Update an existing pet */
    suspend fun updatePet(pet: Pet): Result<Unit>

    /** Delete a pet and its image from Storage */
    suspend fun deletePet(petId: String, imageUrl: String): Result<Unit>

    /** Upload pet image to Firebase Storage — returns download URL */
    suspend fun uploadPetImage(petId: String, imageBytes: ByteArray): Result<String>

    suspend fun getAllPets(): Result<List<Pet>>
}

