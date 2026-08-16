package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_splash
        )

        lifecycleScope.launch {

            delay(1500)

            routeUser()
        }
    }


    private suspend fun routeUser() {

        // User is not signed in
        if (!authRepository.isLoggedIn()) {

            openWelcome()
            return
        }


        /*
         * Phone-only authentication does not
         * require email verification.
         */
        if (!authRepository.isCurrentUserEmailAuth()) {

            openMain()
            return
        }


        /*
         * Email/password account.
         * Refresh Firebase user before trusting
         * email-verification state.
         */
        val refreshedUser =
            authRepository.reloadAndGetUser()


        val verified =
            if (refreshedUser.isSuccess) {

                refreshedUser
                    .getOrThrow()
                    .isEmailVerified

            } else {

                authRepository
                    .isCurrentUserEmailVerified()
            }


        if (verified) {

            openMain()

        } else {

            openEmailVerification()
        }
    }


    private fun openWelcome() {

        startActivity(
            Intent(
                this,
                WelcomeActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }


    private fun openMain() {

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }


    private fun openEmailVerification() {

        startActivity(
            Intent(
                this,
                OtpActivity::class.java
            ).apply {

                putExtra(
                    "type",
                    "email"
                )

                putExtra(
                    "email",
                    authRepository
                        .getCurrentUserEmail()
                        .orEmpty()
                )

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }
}