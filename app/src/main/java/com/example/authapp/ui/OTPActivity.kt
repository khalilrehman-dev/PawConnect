package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.ui.main.MainActivity
import com.google.android.material.appbar.MaterialToolbar
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
import javax.inject.Inject


@AndroidEntryPoint
class OtpActivity :
    AppCompatActivity() {

    @Inject
    lateinit var authRepository:
            AuthRepository


    private lateinit var root:
            View

    private lateinit var toolbar:
            MaterialToolbar


    private lateinit var tvVerificationTitle:
            TextView

    private lateinit var tvOtpSentTo:
            TextView

    private lateinit var tvOtpLabel:
            TextView


    private lateinit var tilOtp:
            TextInputLayout

    private lateinit var etOtp:
            TextInputEditText


    private lateinit var btnVerify:
            MaterialButton

    private lateinit var btnOpenMail:
            MaterialButton


    private lateinit var tvResend:
            TextView

    private lateinit var progressBar:
            CircularProgressIndicator


    private var otpType =
        "email"

    private var userEmail =
        ""

    private var userName =
        ""

    private var userRole =
        ""

    private var userPhone =
        ""

    private var verificationId =
        ""


    private var resendTimer:
            CountDownTimer? =
        null


    private var isBusy =
        false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_otp
        )


        window.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        )


        readIntent()

        bindViews()

        applySystemInsets()

        setupToolbar()

        setupUiForType()
    }


    private fun readIntent() {

        otpType =
            intent
                .getStringExtra(
                    "type"
                )
                ?: "email"


        userEmail =
            intent
                .getStringExtra(
                    "email"
                )
                .orEmpty()


        userName =
            intent
                .getStringExtra(
                    "name"
                )
                .orEmpty()


        userRole =
            intent
                .getStringExtra(
                    "role"
                )
                .orEmpty()


        userPhone =
            intent
                .getStringExtra(
                    "phone"
                )
                .orEmpty()


        verificationId =
            intent
                .getStringExtra(
                    "verificationId"
                )
                .orEmpty()
    }


    private fun bindViews() {

        root =
            findViewById(
                R.id.rootOtp
            )


        toolbar =
            findViewById(
                R.id.toolbarOtp
            )


        tvVerificationTitle =
            findViewById(
                R.id.tvVerificationTitle
            )


        tvOtpSentTo =
            findViewById(
                R.id.tvOtpSentTo
            )


        tvOtpLabel =
            findViewById(
                R.id.tvOtpLabel
            )


        tilOtp =
            findViewById(
                R.id.tilOtp
            )


        etOtp =
            findViewById(
                R.id.etOtp
            )


        btnVerify =
            findViewById(
                R.id.btnVerify
            )


        btnOpenMail =
            findViewById(
                R.id.btnOpenMail
            )


        tvResend =
            findViewById(
                R.id.tvResend
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


    private fun setupToolbar() {

        toolbar.setNavigationOnClickListener {

            onBackPressedDispatcher
                .onBackPressed()
        }
    }


    private fun setupUiForType() {

        when (
            otpType
        ) {

            "email" -> {

                setupEmailVerification()
            }


            "phone_signup",
            "phone_login" -> {

                setupPhoneVerification()
            }


            else -> {

                tvVerificationTitle.text =
                    "Unable to verify"


                tvOtpSentTo.text =
                    "Verification information is missing."


                tilOtp.visibility =
                    View.GONE


                tvOtpLabel.visibility =
                    View.GONE


                btnOpenMail.visibility =
                    View.GONE


                btnVerify.isEnabled =
                    false


                tvResend.visibility =
                    View.GONE
            }
        }
    }


    private fun setupEmailVerification() {

        tvVerificationTitle.text =
            "Verify your email"


        tvOtpSentTo.text =
            if (
                userEmail.isBlank()
            ) {

                "Open the verification email sent to your account and tap the verification link."

            } else {

                "We sent a verification link to:\n$userEmail\n\nOpen the link, then return here."
            }


        tvOtpLabel.visibility =
            View.GONE


        tilOtp.visibility =
            View.GONE


        btnOpenMail.visibility =
            View.VISIBLE


        btnVerify.text =
            "I've Verified — Continue"


        btnVerify.setOnClickListener {

            verifyEmail()
        }


        btnOpenMail.setOnClickListener {

            openEmailApp()
        }


        tvResend.setOnClickListener {

            resendEmailVerification()
        }


        startResendTimer(
            phone = false
        )
    }


    private fun setupPhoneVerification() {

        tvVerificationTitle.text =
            "Verify your phone"


        tvOtpSentTo.text =
            if (
                userPhone.isBlank()
            ) {

                "Enter the 6-digit verification code."

            } else {

                "Enter the 6-digit code sent to:\n$userPhone"
            }


        tvOtpLabel.visibility =
            View.VISIBLE


        tilOtp.visibility =
            View.VISIBLE


        btnOpenMail.visibility =
            View.GONE


        btnVerify.text =
            "Verify Code"


        btnVerify.setOnClickListener {

            verifyPhoneOtp()
        }


        tvResend.setOnClickListener {

            resendPhoneOtp()
        }


        startResendTimer(
            phone = true
        )
    }


    private fun verifyEmail() {

        if (
            isBusy
        ) {

            return
        }


        showLoading(
            true
        )


        lifecycleScope.launch {

            /*
             * One fresh Firebase reload is enough.
             *
             * Do not keep the user waiting through an
             * artificial multi-second polling loop.
             */
            val result =
                authRepository
                    .reloadAndGetUser()


            showLoading(
                false
            )


            if (
                result.isSuccess &&
                result
                    .getOrThrow()
                    .isEmailVerified
            ) {

                proceedAfterVerification()

            } else {

                Toast.makeText(
                    this@OtpActivity,
                    "Your email is not verified yet. Open the verification link, then try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun resendEmailVerification() {

        if (
            isBusy ||
            !tvResend.isEnabled
        ) {

            return
        }


        showLoading(
            true
        )


        lifecycleScope.launch {

            val result =
                authRepository
                    .sendEmailVerification()


            showLoading(
                false
            )


            if (
                result.isSuccess
            ) {

                Toast.makeText(
                    this@OtpActivity,
                    "Verification email sent again.",
                    Toast.LENGTH_SHORT
                ).show()


                startResendTimer(
                    phone = false
                )

            } else {

                Toast.makeText(
                    this@OtpActivity,
                    result
                        .exceptionOrNull()
                        ?.message
                        ?: "Unable to resend verification email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun openEmailApp() {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_MAIN
                ).apply {

                    addCategory(
                        Intent.CATEGORY_APP_EMAIL
                    )


                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
            )

        } catch (
            exception: Exception
        ) {

            Toast.makeText(
                this,
                "No email app was found on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun verifyPhoneOtp() {

        if (
            isBusy
        ) {

            return
        }


        val code =
            etOtp
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        if (
            code.length != 6 ||
            !code.all {
                    character ->
                character.isDigit()
            }
        ) {

            tilOtp.error =
                "Enter the 6-digit code"


            return
        }


        if (
            verificationId.isBlank()
        ) {

            tilOtp.error =
                "Verification session expired. Request a new code."


            return
        }


        tilOtp.error =
            null


        showLoading(
            true
        )


        lifecycleScope.launch {

            val result =
                authRepository
                    .signInWithPhoneCredential(
                        verificationId =
                            verificationId,

                        smsCode =
                            code,

                        displayName =
                            userName,

                        role =
                            userRole
                    )


            showLoading(
                false
            )


            if (
                result.isSuccess
            ) {

                proceedAfterVerification()

            } else {

                tilOtp.error =
                    result
                        .exceptionOrNull()
                        ?.message
                        ?: "Invalid verification code"
            }
        }
    }


    private fun resendPhoneOtp() {

        if (
            isBusy ||
            !tvResend.isEnabled
        ) {

            return
        }


        if (
            userPhone.isBlank()
        ) {

            Toast.makeText(
                this,
                "Phone number is missing.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


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
                        userPhone
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
                                 * Explicit OTP entry remains
                                 * the single flow.
                                 */
                            }


                            override fun onVerificationFailed(
                                exception:
                                FirebaseException
                            ) {

                                showLoading(
                                    false
                                )


                                Toast.makeText(
                                    this@OtpActivity,
                                    exception.message
                                        ?: "Unable to resend code.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }


                            override fun onCodeSent(
                                newVerificationId:
                                String,

                                token:
                                PhoneAuthProvider
                                .ForceResendingToken
                            ) {

                                verificationId =
                                    newVerificationId


                                showLoading(
                                    false
                                )


                                Toast.makeText(
                                    this@OtpActivity,
                                    "Verification code sent again.",
                                    Toast.LENGTH_SHORT
                                ).show()


                                startResendTimer(
                                    phone = true
                                )
                            }
                        }
                    )
                    .build()
            )
    }


    private fun proceedAfterVerification() {

        resendTimer?.cancel()


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


    private fun startResendTimer(
        phone: Boolean
    ) {

        resendTimer?.cancel()


        tvResend.isEnabled =
            false


        resendTimer =
            object :
                CountDownTimer(
                    30_000L,
                    1_000L
                ) {


                override fun onTick(
                    millisUntilFinished:
                    Long
                ) {

                    tvResend.text =
                        "Resend in ${millisUntilFinished / 1_000}s"
                }


                override fun onFinish() {

                    tvResend.text =
                        if (
                            phone
                        ) {

                            "Resend code"

                        } else {

                            "Resend verification email"
                        }


                    tvResend.isEnabled =
                        true
                }
            }
                .start()
    }


    private fun showLoading(
        loading: Boolean
    ) {

        isBusy =
            loading


        progressBar.visibility =
            if (
                loading
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        btnVerify.isEnabled =
            !loading


        btnOpenMail.isEnabled =
            !loading


        etOtp.isEnabled =
            !loading


        if (
            loading
        ) {

            tvResend.isEnabled =
                false
        }
    }


    override fun onDestroy() {

        resendTimer?.cancel()


        super.onDestroy()
    }
}