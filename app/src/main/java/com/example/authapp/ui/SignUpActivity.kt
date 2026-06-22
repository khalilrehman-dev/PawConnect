package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.authapp.R
import com.example.authapp.presentation.auth.SignUpEvent
import com.example.authapp.presentation.auth.SignUpUiState
import com.example.authapp.presentation.auth.SignUpViewModel
import com.example.authapp.utils.ValidationUtils
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
class SignUpActivity : AppCompatActivity() {

    private val viewModel: SignUpViewModel by viewModels()

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tvPasswordHint: TextView
    private lateinit var btnSignUp: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rbPetOwner: RadioButton
    private lateinit var rbVet: RadioButton
    private lateinit var rbSignUpEmail: RadioButton
    private lateinit var rbSignUpPhone: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        bindViews()
        setupMethodToggle()
        observeViewModel()

        btnSignUp.setOnClickListener {
            val role = if (rbPetOwner.isChecked) "pet_owner" else "veterinarian"
            if (rbSignUpEmail.isChecked) {
                viewModel.registerWithEmail(
                    name     = etName.text.toString().trim(),
                    email    = etEmail.text.toString().trim(),
                    password = etPassword.text.toString(),
                    role     = role
                )
            } else {
                // ViewModel validates first, then fires StartPhoneVerification event
                viewModel.validatePhoneSignup(
                    name  = etName.text.toString().trim(),
                    phone = etPhone.text.toString().trim(),
                    role  = role
                )
            }
        }

        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun bindViews() {
        etName         = findViewById(R.id.etName)
        etEmail        = findViewById(R.id.etEmail)
        etPassword     = findViewById(R.id.etPassword)
        etPhone        = findViewById(R.id.etPhone)
        tilName        = findViewById(R.id.tilName)
        tilEmail       = findViewById(R.id.tilEmail)
        tilPassword    = findViewById(R.id.tilPassword)
        tilPhone       = findViewById(R.id.tilPhone)
        tvPasswordHint = findViewById(R.id.tvPasswordHint)
        btnSignUp      = findViewById(R.id.btnSignUp)
        progressBar    = findViewById(R.id.progressBar)
        rbPetOwner     = findViewById(R.id.rbPetOwner)
        rbVet          = findViewById(R.id.rbVet)
        rbSignUpEmail  = findViewById(R.id.rbSignUpEmail)
        rbSignUpPhone  = findViewById(R.id.rbSignUpPhone)
    }

    private fun setupMethodToggle() {
        findViewById<RadioGroup>(R.id.rgSignUpMethod)
            .setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == R.id.rbSignUpEmail) {
                    tilEmail.visibility       = View.VISIBLE
                    tilPassword.visibility    = View.VISIBLE
                    tvPasswordHint.visibility = View.VISIBLE
                    tilPhone.visibility       = View.GONE
                } else {
                    tilEmail.visibility       = View.GONE
                    tilPassword.visibility    = View.GONE
                    tvPasswordHint.visibility = View.GONE
                    tilPhone.visibility       = View.VISIBLE
                }
            }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is SignUpUiState.Idle    -> showLoading(false)
                            is SignUpUiState.Loading -> showLoading(true)
                            is SignUpUiState.Error   -> {
                                showLoading(false)
                                Toast.makeText(this@SignUpActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SignUpEvent.GoToEmailOtp -> {
                                startActivity(
                                    Intent(this@SignUpActivity, OtpActivity::class.java).apply {
                                        putExtra("type",  "email")
                                        putExtra("email", event.email)
                                        putExtra("name",  event.name)
                                        putExtra("role",  event.role)
                                    }
                                )
                            }
                            is SignUpEvent.StartPhoneVerification -> {
                                // Validation passed — now trigger Firebase from Activity
                                initiatePhoneSignup(
                                    phone = event.phone,
                                    name  = event.name,
                                    role  = event.role
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initiatePhoneSignup(phone: String, name: String, role: String) {
        showLoading(true)
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(c: PhoneAuthCredential) {
                        // Auto-retrieval not used for test numbers — ignore
                    }
                    override fun onVerificationFailed(e: FirebaseException) {
                        showLoading(false)
                        Toast.makeText(this@SignUpActivity, e.message ?: "Phone verification failed", Toast.LENGTH_LONG).show()
                    }
                    override fun onCodeSent(vId: String, t: PhoneAuthProvider.ForceResendingToken) {
                        showLoading(false)
                        startActivity(
                            Intent(this@SignUpActivity, OtpActivity::class.java).apply {
                                putExtra("type",           "phone_signup")
                                putExtra("phone",          phone)
                                putExtra("name",           name)
                                putExtra("role",           role)
                                putExtra("verificationId", vId)
                            }
                        )
                    }
                }).build()
        )
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSignUp.isEnabled    = !show
    }
}