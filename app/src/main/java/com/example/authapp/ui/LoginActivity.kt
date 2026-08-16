package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.authapp.R
import com.example.authapp.presentation.auth.LoginEvent
import com.example.authapp.presentation.auth.LoginUiState
import com.example.authapp.presentation.auth.LoginViewModel
import com.example.authapp.ui.main.MainActivity
import com.example.authapp.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class LoginActivity :
    AppCompatActivity() {

    private val viewModel:
            LoginViewModel by viewModels()


    private lateinit var root:
            View


    private lateinit var rgLoginMethod:
            RadioGroup

    private lateinit var rbEmail:
            RadioButton


    private lateinit var etEmail:
            TextInputEditText

    private lateinit var etPhone:
            TextInputEditText

    private lateinit var etPassword:
            TextInputEditText


    private lateinit var tilEmail:
            TextInputLayout

    private lateinit var tilPhone:
            TextInputLayout

    private lateinit var tilPassword:
            TextInputLayout


    private lateinit var btnLogin:
            MaterialButton

    private lateinit var progressBar:
            CircularProgressIndicator


    private lateinit var tvSignUp:
            TextView


    private var phoneVerificationRunning =
        false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_login
        )


        window.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        )


        bindViews()

        applySystemInsets()

        setupToggle()

        setupClicks()

        observeViewModel()
    }


    private fun bindViews() {

        root =
            findViewById(
                R.id.rootLogin
            )


        rgLoginMethod =
            findViewById(
                R.id.rgLoginMethod
            )


        rbEmail =
            findViewById(
                R.id.rbEmail
            )


        tilEmail =
            findViewById(
                R.id.tilEmail
            )


        tilPhone =
            findViewById(
                R.id.tilPhone
            )


        tilPassword =
            findViewById(
                R.id.tilPassword
            )


        etEmail =
            findViewById(
                R.id.etEmail
            )


        etPhone =
            findViewById(
                R.id.etPhone
            )


        etPassword =
            findViewById(
                R.id.etPassword
            )


        btnLogin =
            findViewById(
                R.id.btnLogin
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )


        tvSignUp =
            findViewById(
                R.id.tvSignUp
            )
    }


    private fun applySystemInsets() {

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


    private fun setupToggle() {

        rgLoginMethod
            .setOnCheckedChangeListener { _, _ ->

                clearFieldErrors()

                updateMethodUi()
            }


        updateMethodUi()
    }


    private fun updateMethodUi() {

        val emailLogin =
            rbEmail.isChecked


        tilEmail.visibility =
            if (
                emailLogin
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        tilPassword.visibility =
            if (
                emailLogin
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        tilPhone.visibility =
            if (
                emailLogin
            ) {

                View.GONE

            } else {

                View.VISIBLE
            }


        btnLogin.text =
            if (
                emailLogin
            ) {

                "Sign In"

            } else {

                "Send Verification Code"
            }
    }


    private fun setupClicks() {

        btnLogin.setOnClickListener {

            clearFieldErrors()


            if (
                rbEmail.isChecked
            ) {

                viewModel.loginWithEmail(
                    email =
                        etEmail
                            .text
                            ?.toString()
                            ?.trim()
                            .orEmpty(),

                    password =
                        etPassword
                            .text
                            ?.toString()
                            .orEmpty()
                )

            } else {

                initiatePhoneLogin()
            }
        }


        tvSignUp.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SignUpActivity::class.java
                )
            )


            finish()
        }
    }


    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel
                        .uiState
                        .collect { state ->

                            when (state) {

                                LoginUiState.Idle -> {

                                    if (
                                        !phoneVerificationRunning
                                    ) {

                                        showLoading(
                                            false
                                        )
                                    }
                                }


                                LoginUiState.Loading -> {

                                    showLoading(
                                        true
                                    )
                                }


                                is LoginUiState.Error -> {

                                    showLoading(
                                        false
                                    )


                                    showLoginError(
                                        state.message
                                    )
                                }
                            }
                        }
                }


                launch {

                    viewModel
                        .events
                        .collect { event ->

                            when (event) {

                                is LoginEvent.NavigateTo -> {

                                    openMain()
                                }
                            }
                        }
                }
            }
        }
    }


    private fun initiatePhoneLogin() {

        val phone =
            etPhone
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        if (
            !ValidationUtils
                .isValidPhone(
                    phone
                )
        ) {

            tilPhone.error =
                "Enter a valid phone number"


            return
        }


        tilPhone.error =
            null


        phoneVerificationRunning =
            true


        showLoading(
            true
        )


        val formattedPhone =
            ValidationUtils
                .formatPhoneForFirebase(
                    phone
                )


        PhoneAuthProvider
            .verifyPhoneNumber(

                PhoneAuthOptions
                    .newBuilder(
                        FirebaseAuth.getInstance()
                    )
                    .setPhoneNumber(
                        formattedPhone
                    )
                    .setTimeout(
                        60L,
                        TimeUnit.SECONDS
                    )
                    .setActivity(
                        this
                    )
                    .setCallbacks(

                        object :
                            PhoneAuthProvider
                            .OnVerificationStateChangedCallbacks() {


                            override fun onVerificationCompleted(
                                credential:
                                PhoneAuthCredential
                            ) {

                                /*
                                 * Current architecture uses the
                                 * explicit OTP screen.
                                 *
                                 * Do not create another auth path here.
                                 */
                            }


                            override fun onVerificationFailed(
                                exception:
                                FirebaseException
                            ) {

                                phoneVerificationRunning =
                                    false


                                showLoading(
                                    false
                                )


                                Toast.makeText(
                                    this@LoginActivity,
                                    exception.message
                                        ?: "Phone verification failed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }


                            override fun onCodeSent(
                                verificationId:
                                String,

                                token:
                                PhoneAuthProvider
                                .ForceResendingToken
                            ) {

                                phoneVerificationRunning =
                                    false


                                showLoading(
                                    false
                                )


                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        OtpActivity::class.java
                                    ).apply {

                                        putExtra(
                                            "type",
                                            "phone_login"
                                        )


                                        putExtra(
                                            "phone",
                                            formattedPhone
                                        )


                                        putExtra(
                                            "verificationId",
                                            verificationId
                                        )
                                    }
                                )
                            }
                        }
                    )
                    .build()
            )
    }


    private fun showLoginError(
        message: String
    ) {

        val lowercase =
            message.lowercase()


        when {

            lowercase.contains(
                "valid email"
            ) -> {

                tilEmail.error =
                    message
            }


            lowercase.contains(
                "password"
            ) -> {

                tilPassword.error =
                    message
            }


            else -> {

                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun clearFieldErrors() {

        tilEmail.error =
            null


        tilPhone.error =
            null


        tilPassword.error =
            null
    }


    private fun showLoading(
        loading: Boolean
    ) {

        progressBar.visibility =
            if (
                loading
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        btnLogin.isEnabled =
            !loading


        rgLoginMethod.isEnabled =
            !loading


        etEmail.isEnabled =
            !loading


        etPhone.isEnabled =
            !loading


        etPassword.isEnabled =
            !loading


        btnLogin.text =
            if (
                loading
            ) {

                if (
                    rbEmail.isChecked
                ) {

                    "Signing In..."

                } else {

                    "Sending Code..."
                }

            } else {

                if (
                    rbEmail.isChecked
                ) {

                    "Sign In"

                } else {

                    "Send Verification Code"
                }
            }
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
}