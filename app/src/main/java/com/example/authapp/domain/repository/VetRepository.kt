package com.example.authapp.domain.repository

import com.example.authapp.model.Vet

interface VetRepository {
    suspend fun saveVetProfile(vet: Vet): Result<Unit>
    suspend fun getVetById(uid: String): Result<Vet>
    suspend fun getAllVets(): Result<List<Vet>>
}