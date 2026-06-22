package com.example.authapp.model

data class Vet(
    val uid: String = "",
    val displayName: String = "",
    val clinicName: String = "",
    val city: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val specialization: String = "",
    val yearsOfExperience: Int = 0,
    val profileImageUrl: String = "",
    val isAvailable: Boolean = true,
    val createdAt: Long = 0L
)