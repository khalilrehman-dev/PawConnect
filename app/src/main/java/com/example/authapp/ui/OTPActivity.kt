package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.ui.Vets.VetProfileSetupActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class OtpActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    private lateinit var tvOtpSentTo: TextView
    private lateinit var tvOtpLabel: TextView
    private lateinit var tilOtp: TextInputLayout
    private lateinit var etOtp: TextInputEditText
    private lateinit var btnVerify: Button
    private lateinit var btnOpenMail: Button
    private lateinit var tvResend: TextView
    private lateinit var progressBar: ProgressBar

    private var otpType: String        = "email"
    private var userEmail: String      = ""
    private var userName: String       = ""
    private var userRole: String       = ""
    private var userPhone: String      = ""
    private var verificationId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        otpType        = intent.getStringExtra("type")           ?: "email"
        userEmail      = intent.getStringExtra("email")          ?: ""
        userName       = intent.getStringExtra("name")           ?: ""
        userRole       = intent.getStringExtra("role")           ?: ""
        userPhone      = intent.getStringExtra("phone")          ?: ""
        verificationId = intent.getStringExtra("verificationId") ?: ""

        bindViews()
        setupUiForType()
    }

    private fun bindViews() {
        tvOtpSentTo = findViewById(R.id.tvOtpSentTo)
        tvOtpLabel  = findViewById(R.id.tvOtpLabel)
        tilOtp      = findViewById(R.id.tilOtp)
        etOtp       = findViewById(R.id.etOtp)
        btnVerify   = findViewById(R.id.btnVerify)
        btnOpenMail = findViewById(R.id.btnOpenMail)
        tvResend    = findViewById(R.id.tvResend)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupUiForType() {
        when (otpType) {

            "email" -> {
                tvOtpSentTo.text =
                    "A verification link was sent to:\n$userEmail\n\n" +
                            "Steps:\n" +
                            "1. Open your Gmail app\n" +
                            "2. Check Spam / Promotions folder\n" +
                            "3. Click the verification link\n" +
                            "4. Come back and press Continue"

                tilOtp.visibility      = View.GONE
                tvOtpLabel.visibility  = View.GONE
                btnOpenMail.visibility = View.VISIBLE
                btnVerify.text         = "I've Verified — Continue"

                btnVerify.setOnClickListener   { verifyEmail() }
                btnOpenMail.setOnClickListener { openEmailApp() }

                startResendTimer(isPhone = false)
                tvResend.setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        authRepository.sendEmailVerification()
                        Toast.makeText(
                            this@OtpActivity,
                            "Verification email resent. Check spam.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    startResendTimer(isPhone = false)
                }
            }

            "phone_signup", "phone_login" -> {
                tvOtpSentTo.text       = "Enter the 6-digit OTP sent to\n$userPhone"
                tilOtp.visibility      = View.VISIBLE
                tvOtpLabel.visibility  = View.VISIBLE
                btnOpenMail.visibility = View.GONE
                btnVerify.text         = "Verify OTP"

                btnVerify.setOnClickListener { verifyPhoneOtp() }

                startResendTimer(isPhone = true)
                tvResend.setOnClickListener {
                    resendPhoneOtp()
                    startResendTimer(isPhone = true)
                }
            }
        }
    }

    // ── Email verification ────────────────────────────────────────────────────

    private fun verifyEmail() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            var verified = false
            repeat(6) { attempt ->
                if (verified) return@repeat
                try {
                    // Force a fresh Firebase instance reload each time
                    FirebaseAuth.getInstance().currentUser?.reload()?.await()
                    val isVerified = FirebaseAuth.getInstance().currentUser?.isEmailVerified ?: false
                    if (isVerified) {
                        verified = true
                    }
                } catch (e: Exception) {
                    // ignore and retry
                }
                if (!verified) delay(3000)
            }
            withContext(Dispatchers.Main) {
                showLoading(false)
                if (verified) {
                    proceedAfterVerification()
                } else {
                    Toast.makeText(
                        this@OtpActivity,
                        "Not verified yet. Open Gmail app → Spam → click the link, then try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openEmailApp() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Phone OTP ─────────────────────────────────────────────────────────────

    private fun verifyPhoneOtp() {
        val code = etOtp.text.toString().trim()
        if (code.length != 6) {
            tilOtp.error = "Enter the 6-digit OTP"
            return
        }
        tilOtp.error = null
        showLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            val result = authRepository.signInWithPhoneCredential(
                verificationId = verificationId,
                smsCode        = code,
                displayName    = userName,
                role           = userRole
            )
            showLoading(false)
            if (result.isSuccess) {
                // ✅ Fixed — was incorrectly calling verifyEmail() before
                proceedAfterVerification()
            } else {
                tilOtp.error = result.exceptionOrNull()?.message ?: "Invalid OTP"
            }
        }
    }

    private fun resendPhoneOtp() {
        if (userPhone.isEmpty()) return
        showLoading(true)
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(userPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(c: PhoneAuthCredential) {}
                    override fun onVerificationFailed(e: FirebaseException) {
                        showLoading(false)
                        Toast.makeText(
                            this@OtpActivity,
                            e.message ?: "Failed to resend",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    override fun onCodeSent(vId: String, t: PhoneAuthProvider.ForceResendingToken) {
                        showLoading(false)
                        verificationId = vId
                        Toast.makeText(this@OtpActivity, "OTP resent", Toast.LENGTH_SHORT).show()
                    }
                }).build()
        )
    }

    // ── Navigation after verification ─────────────────────────────────────────

    private fun proceedAfterVerification() {
        CoroutineScope(Dispatchers.Main).launch {
            val uid = authRepository.getCurrentUid()
            if (uid != null) {
                val result = authRepository.getUserFromFirestore(uid)
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    if (user.role == "veterinarian") {
                        startActivity(
                            Intent(this@OtpActivity, VetProfileSetupActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                        return@launch
                    }
                }
            }
            startActivity(Intent(this@OtpActivity, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startResendTimer(isPhone: Boolean) {
        tvResend.isEnabled = false
        object : CountDownTimer(30_000, 1_000) {
            override fun onTick(ms: Long) { tvResend.text = "Resend in ${ms / 1000}s" }
            override fun onFinish() {
                tvResend.text      = if (isPhone) "Resend OTP" else "Resend email"
                tvResend.isEnabled = true
            }
        }.start()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnVerify.isEnabled    = !show
    }
}