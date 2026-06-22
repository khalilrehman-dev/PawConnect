package com.example.authapp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "AuthAppSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_PHONE = "userPhone"
        private const val KEY_USER_NAME = "userName"
    }

    // Save login state
    fun setLoggedIn(
        isLoggedIn: Boolean,
        name: String = "",
        email: String = "",
        phone: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PHONE, phone)
            apply()
        }
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Get saved user details
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""

    // Clear session on logout
    fun logout() {
        prefs.edit().clear().apply()
    }
}