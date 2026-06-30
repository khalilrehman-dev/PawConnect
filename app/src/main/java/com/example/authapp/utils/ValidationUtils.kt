package com.example.authapp.utils

object ValidationUtils {

    // Check if email format is valid
    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return emailRegex.matches(email)
    }

    // Check if phone number is valid (10 digits minimum)
    fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.trim()
        return cleaned.length >= 10 && cleaned.all { it.isDigit() }
    }

    // Check password strength
    // Must be at least 8 chars, 1 uppercase, 1 number
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isDigit() }) return false
        return true
    }

    // Get password weakness reason as a message
    fun getPasswordError(password: String): String {
        return when {
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password must contain at least 1 uppercase letter"
            !password.any { it.isDigit() } -> "Password must contain at least 1 number"
            else -> ""
        }
    }

    // Check if name is valid (not empty, at least 2 chars)
    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    // Format phone number for Firebase (+92 for Pakistan, adjust as needed)
    fun formatPhoneForFirebase(phone: String): String {
        val cleaned = phone.trim()
        return if (cleaned.startsWith("+")) cleaned else "+92$cleaned"
    }
}