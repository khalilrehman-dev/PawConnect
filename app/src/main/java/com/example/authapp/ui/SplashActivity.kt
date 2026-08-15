package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

        lifecycleScope.launch {
            delay(1500)
            routeUser()
        }


    }
    private suspend fun routeUser() {

        if (!authRepository.isLoggedIn()) {
            openWelcome()
            return
        }

        // Phone-only user
        if (!authRepository.isCurrentUserEmailAuth()) {
            openDashboard()
            return
        }

        // Email/password user
        val refreshedUser = authRepository.reloadAndGetUser()

        val verified =
            if (refreshedUser.isSuccess) {
                refreshedUser.getOrThrow().isEmailVerified
            } else {
                authRepository.isCurrentUserEmailVerified()
            }

        if (verified) {
            openDashboard()
        } else {
            openEmailVerification()
        }
    }
    private fun openWelcome() {
        startActivity(
            Intent(this, WelcomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun openDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun openEmailVerification() {
        startActivity(
            Intent(this, OtpActivity::class.java).apply {
                putExtra("type", "email")
                putExtra(
                    "email",
                    authRepository.getCurrentUserEmail().orEmpty()
                )

                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}