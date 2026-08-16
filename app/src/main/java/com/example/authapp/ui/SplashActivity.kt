package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SplashActivity :
    AppCompatActivity() {

    @Inject
    lateinit var authRepository:
            AuthRepository


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_splash
        )


        applySystemInsets()


        lifecycleScope.launch {

            /*
             * Very small delay prevents a harsh flash
             * for logged-out users while keeping startup fast.
             */
            delay(
                500
            )


            routeUser()
        }
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootSplash
            )


        ViewCompat
            .setOnApplyWindowInsetsListener(
                root
            ) { view, insets ->

                val systemBars =
                    insets.getInsets(
                        WindowInsetsCompat
                            .Type
                            .systemBars()
                    )


                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )


                insets
            }
    }


    private suspend fun routeUser() {

        /*
         * No Firebase session.
         */
        if (
            !authRepository.isLoggedIn()
        ) {

            openWelcome()

            return
        }


        /*
         * Phone-auth users don't require
         * Firebase email verification.
         */
        if (
            !authRepository
                .isCurrentUserEmailAuth()
        ) {

            openMain()

            return
        }


        /*
         * Email/password users must be checked
         * against a freshly reloaded Firebase user.
         */
        val refreshedUser =
            authRepository
                .reloadAndGetUser()


        val verified =
            if (
                refreshedUser.isSuccess
            ) {

                refreshedUser
                    .getOrThrow()
                    .isEmailVerified

            } else {

                authRepository
                    .isCurrentUserEmailVerified()
            }


        if (
            verified
        ) {

            openMain()

        } else {

            openEmailVerification()
        }
    }


    private fun openWelcome() {

        openCleared(
            WelcomeActivity::class.java
        )
    }


    private fun openMain() {

        openCleared(
            MainActivity::class.java
        )
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


    private fun openCleared(
        destination: Class<*>
    ) {

        startActivity(
            Intent(
                this,
                destination
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )


        finish()
    }
}