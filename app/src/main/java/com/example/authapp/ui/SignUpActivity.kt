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
import com.example.authapp.presentation.auth.SignUpEvent
import com.example.authapp.presentation.auth.SignUpUiState
import com.example.authapp.presentation.auth.SignUpViewModel
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
class SignUpActivity :
    AppCompatActivity() {

    private val viewModel:
            SignUpViewModel by viewModels()


    private lateinit var root:
            View


    private lateinit var rgRole:
            RadioGroup

    private lateinit var rbPetOwner:
            RadioButton

    private lateinit var rbVet:
            RadioButton


    private lateinit var rgSignUpMethod:
            RadioGroup

    private lateinit var rbSignUpEmail:
            RadioButton


    private lateinit var tilName:
            TextInputLayout

    private lateinit var tilEmail:
            TextInputLayout

    private lateinit var tilPassword:
            TextInputLayout

    private lateinit var tilPhone:
            TextInputLayout


    private lateinit var etName:
            TextInputEditText

    private lateinit var etEmail:
            TextInputEditText

    private lateinit var etPassword:
            TextInputEditText

    private lateinit var etPhone:
            TextInputEditText


    private lateinit var tvPasswordHint:
            TextView

    private lateinit var tvLogin:
            TextView


    private lateinit var btnSignUp:
            MaterialButton

    private lateinit var progressBar:
            CircularProgressIndicator


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
            R.layout.activity_sign_up
        )


        window.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        )


        bindViews()

        applySystemInsets()

        setupMethodToggle()

        setupClicks()

        observeViewModel()
    }


    private fun bindViews() {

        root =
            findViewById(
                R.id.rootSignUp
            )


        rgRole =
            findViewById(
                R.id.rgRole
            )


        rbPetOwner =
            findViewById(
                R.id.rbPetOwner
            )


        rbVet =
            findViewById(
                R.id.rbVet
            )


        rgSignUpMethod =
            findViewById(
                R.id.rgSignUpMethod
            )


        rbSignUpEmail =
            findViewById(
                R.id.rbSignUpEmail
            )


        tilName =
            findViewById(
                R.id.tilName
            )


        tilEmail =
            findViewById(
                R.id.tilEmail
            )


        tilPassword =
            findViewById(
                R.id.tilPassword
            )


        tilPhone =
            findViewById(
                R.id.tilPhone
            )


        etName =
            findViewById(
                R.id.etName
            )


        etEmail =
            findViewById(
                R.id.etEmail
            )


        etPassword =
            findViewById(
                R.id.etPassword
            )


        etPhone =
            findViewById(
                R.id.etPhone
            )


        tvPasswordHint =
            findViewById(
                R.id.tvPasswordHint
            )


        tvLogin =
            findViewById(
                R.id.tvLogin
            )


        btnSignUp =
            findViewById(
                R.id.btnSignUp
            )


        progressBar =
            findViewById(
                R.id.progressBar
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


    private fun setupMethodToggle() {

        rgSignUpMethod
            .setOnCheckedChangeListener { _, _ ->

                clearErrors()

                updateMethodUi()
            }


        updateMethodUi()
    }


    private fun updateMethodUi() {

        val emailSignup =
            rbSignUpEmail.isChecked


        tilEmail.visibility =
            if (
                emailSignup
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        tilPassword.visibility =
            if (
                emailSignup
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        tvPasswordHint.visibility =
            if (
                emailSignup
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        tilPhone.visibility =
            if (
                emailSignup
            ) {

                View.GONE

            } else {

                View.VISIBLE
            }


        btnSignUp.text =
            if (
                emailSignup
            ) {

                "Create Account"

            } else {

                "Send Verification Code"
            }
    }


    private fun setupClicks() {

        btnSignUp.setOnClickListener {

            clearErrors()


            val role =
                when {

                    rbPetOwner.isChecked ->
                        "pet_owner"

                    rbVet.isChecked ->
                        "veterinarian"

                    else ->
                        ""
                }


            if (
                rbSignUpEmail.isChecked
            ) {

                viewModel.registerWithEmail(
                    name =
                        etName
                            .text
                            ?.toString()
                            ?.trim()
                            .orEmpty(),

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
                            .orEmpty(),

                    role =
                        role
                )

            } else {

                viewModel.validatePhoneSignup(
                    name =
                        etName
                            .text
                            ?.toString()
                            ?.trim()
                            .orEmpty(),

                    phone =
                        etPhone
                            .text
                            ?.toString()
                            ?.trim()
                            .orEmpty(),

                    role =
                        role
                )
            }
        }


        tvLogin.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
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

                                SignUpUiState.Idle -> {

                                    if (
                                        !phoneVerificationRunning
                                    ) {

                                        showLoading(
                                            false
                                        )
                                    }
                                }


                                SignUpUiState.Loading -> {

                                    showLoading(
                                        true
                                    )
                                }


                                is SignUpUiState.Error -> {

                                    showLoading(
                                        false
                                    )


                                    showSignupError(
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

                                is SignUpEvent.GoToEmailOtp -> {

                                    startActivity(
                                        Intent(
                                            this@SignUpActivity,
                                            OtpActivity::class.java
                                        ).apply {

                                            putExtra(
                                                "type",
                                                "email"
                                            )


                                            putExtra(
                                                "email",
                                                event.email
                                            )


                                            putExtra(
                                                "name",
                                                event.name
                                            )


                                            putExtra(
                                                "role",
                                                event.role
                                            )
                                        }
                                    )
                                }


                                is SignUpEvent
                                .StartPhoneVerification -> {

                                    initiatePhoneSignup(
                                        phone =
                                            event.phone,

                                        name =
                                            event.name,

                                        role =
                                            event.role
                                    )
                                }
                            }
                        }
                }
            }
        }
    }


    private fun initiatePhoneSignup(
        phone: String,
        name: String,
        role: String
    ) {

        phoneVerificationRunning =
            true


        showLoading(
            true
        )


        PhoneAuthProvider
            .verifyPhoneNumber(

                PhoneAuthOptions
                    .newBuilder(
                        FirebaseAuth.getInstance()
                    )
                    .setPhoneNumber(
                        phone
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
                                 * Keep one explicit OTP flow.
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
                                    this@SignUpActivity,
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
                                        this@SignUpActivity,
                                        OtpActivity::class.java
                                    ).apply {

                                        putExtra(
                                            "type",
                                            "phone_signup"
                                        )


                                        putExtra(
                                            "phone",
                                            phone
                                        )


                                        putExtra(
                                            "name",
                                            name
                                        )


                                        putExtra(
                                            "role",
                                            role
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


    private fun showSignupError(
        message: String
    ) {

        val lowercase =
            message.lowercase()


        when {

            lowercase.contains(
                "name"
            ) -> {

                tilName.error =
                    message
            }


            lowercase.contains(
                "email"
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


            lowercase.contains(
                "phone"
            ) -> {

                tilPhone.error =
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


    private fun clearErrors() {

        tilName.error =
            null


        tilEmail.error =
            null


        tilPassword.error =
            null


        tilPhone.error =
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


        btnSignUp.isEnabled =
            !loading


        rgRole.isEnabled =
            !loading


        rgSignUpMethod.isEnabled =
            !loading


        etName.isEnabled =
            !loading


        etEmail.isEnabled =
            !loading


        etPassword.isEnabled =
            !loading


        etPhone.isEnabled =
            !loading


        btnSignUp.text =
            if (
                loading
            ) {

                if (
                    rbSignUpEmail.isChecked
                ) {

                    "Creating Account..."

                } else {

                    "Sending Code..."
                }

            } else {

                if (
                    rbSignUpEmail.isChecked
                ) {

                    "Create Account"

                } else {

                    "Send Verification Code"
                }
            }
    }
}