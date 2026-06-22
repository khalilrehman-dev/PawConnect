package com.example.authapp.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val role: String = "",
    val isEmailVerified: Boolean = false,
    val createdAt: Long = 0L
)