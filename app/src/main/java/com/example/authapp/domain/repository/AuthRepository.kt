package com.example.authapp.domain.repository

import com.example.authapp.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String, role: String): Result<User>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadAndGetUser(): Result<User>
    suspend fun signInWithPhoneCredential(
        verificationId: String,
        smsCode: String,
        displayName: String = "",
        role: String = ""
    ): Result<User>
    suspend fun saveUserToFirestore(user: User): Result<Unit>
    suspend fun getUserFromFirestore(uid: String): Result<User>
    suspend fun updateUserRole(uid: String, role: String): Result<Unit>
    suspend fun updateUserProfile(
        uid: String,
        displayName: String,
        phone: String,
        profileImageUrl: String
    ): Result<Unit>    fun isLoggedIn(): Boolean
    fun getCurrentUid(): String?
    suspend fun logout(): Result<Unit>
}