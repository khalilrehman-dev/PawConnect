package com.example.authapp.data.repository

import com.example.authapp.model.User
import com.example.authapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    companion object {
        private const val USERS = "users"
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        val snap = firestore.collection(USERS).document(uid).get().await()
        snap.toUser()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        role: String
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = User(
            uid         = result.user!!.uid,
            email       = email,
            displayName = displayName,
            role        = role,
            createdAt   = System.currentTimeMillis()
        )
        firestore.collection(USERS).document(user.uid).set(user.toMap()).await()
        user
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await() ?: error("No user")
    }

    override suspend fun reloadAndGetUser(): Result<User> = runCatching {
        auth.currentUser?.reload()?.await() ?: error("No user signed in")
        val firebaseUser = auth.currentUser ?: error("No user signed in")
        if (firebaseUser.isEmailVerified) {
            firestore.collection(USERS)
                .document(firebaseUser.uid)
                .update("isEmailVerified", true)
                .await()
        }
        User(
            uid             = firebaseUser.uid,
            email           = firebaseUser.email ?: "",
            isEmailVerified = firebaseUser.isEmailVerified
        )
    }

    // Updated — accepts displayName and role so new phone users get a proper Firestore doc
    override suspend fun signInWithPhoneCredential(
        verificationId: String,
        smsCode: String,
        displayName: String,
        role: String
    ): Result<User> = runCatching {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        val result     = auth.signInWithCredential(credential).await()
        val uid        = result.user!!.uid
        val snap       = firestore.collection(USERS).document(uid).get().await()

        if (snap.exists()) {
            // Returning user — just return their existing data
            snap.toUser()
        } else {
            // New phone signup — create Firestore doc with name + role
            val user = User(
                uid         = uid,
                displayName = displayName,
                phoneNumber = result.user!!.phoneNumber ?: "",
                role        = role,
                createdAt   = System.currentTimeMillis()
            )
            firestore.collection(USERS).document(uid).set(user.toMap()).await()
            user
        }
    }

    override suspend fun saveUserToFirestore(user: User): Result<Unit> = runCatching {
        firestore.collection(USERS).document(user.uid).set(user.toMap()).await()
    }

    override suspend fun getUserFromFirestore(uid: String): Result<User> = runCatching {
        firestore.collection(USERS).document(uid).get().await().toUser()
    }

    override suspend fun updateUserRole(uid: String, role: String): Result<Unit> = runCatching {
        firestore.collection(USERS).document(uid).update("role", role).await()
    }

    override suspend fun updateUserProfile(
        uid: String,
        displayName: String,
        phone: String,
        profileImageUrl: String
    ): Result<Unit> = runCatching {

        firestore.collection(USERS)
            .document(uid)
            .update(
                mapOf(
                    "displayName" to displayName,
                    "phoneNumber" to phone,
                    "profileImageUrl" to profileImageUrl
                )
            )
            .await()
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null
    override fun getCurrentUid(): String? = auth.currentUser?.uid

    override suspend fun logout(): Result<Unit> = runCatching { auth.signOut() }

    private fun User.toMap() = mapOf(
        "uid" to uid, "displayName" to displayName, "email" to email,
        "phoneNumber" to phoneNumber, "profileImageUrl" to profileImageUrl,
        "role" to role, "isEmailVerified" to isEmailVerified, "createdAt" to createdAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUser() = User(
        uid             = getString("uid")             ?: "",
        displayName     = getString("displayName")     ?: "",
        email           = getString("email")           ?: "",
        phoneNumber     = getString("phoneNumber")     ?: "",
        profileImageUrl = getString("profileImageUrl") ?: "",
        role            = getString("role")            ?: "",
        isEmailVerified = getBoolean("isEmailVerified") ?: false,
        createdAt       = getLong("createdAt")         ?: 0L
    )
}