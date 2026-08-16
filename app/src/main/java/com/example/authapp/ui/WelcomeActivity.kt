package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.authapp.R
import com.google.android.material.button.MaterialButton


class WelcomeActivity :
    AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_welcome
        )


        applySystemInsets()


        val btnSignUp =
            findViewById<MaterialButton>(
                R.id.btnSignUp
            )


        val btnLogin =
            findViewById<MaterialButton>(
                R.id.btnLogin
            )


        btnSignUp.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SignUpActivity::class.java
                )
            )
        }


        btnLogin.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootWelcome
            )


        ViewCompat
            .setOnApplyWindowInsetsListener(
                root
            ) { view, insets ->

                val bars =
                    insets.getInsets(
                        WindowInsetsCompat
                            .Type
                            .systemBars()
                    )


                view.setPadding(
                    bars.left,
                    bars.top,
                    bars.right,
                    bars.bottom
                )


                insets
            }
    }
}