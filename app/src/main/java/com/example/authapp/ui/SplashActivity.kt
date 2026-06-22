package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val dest = if (authRepository.isLoggedIn()) DashboardActivity::class.java
            else WelcomeActivity::class.java
            startActivity(Intent(this, dest).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }, 1500)

        // TESTING ONLY — remove before publishing
        FirebaseAuth.getInstance().firebaseAuthSettings
            .setAppVerificationDisabledForTesting(true)
    }
}