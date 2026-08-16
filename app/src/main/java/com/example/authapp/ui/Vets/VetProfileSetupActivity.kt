package com.example.authapp.ui.Vets

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.authapp.R
import com.example.authapp.data.remote.CloudinaryUploader
import com.example.authapp.domain.repository.VetRepository
import com.example.authapp.presentation.vets.VetProfileSetupViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class VetProfileSetupActivity :
    AppCompatActivity() {

    @Inject
    lateinit var vetRepository:
            VetRepository


    @Inject
    lateinit var auth:
            FirebaseAuth


    @Inject
    lateinit var cloudinaryUploader:
            CloudinaryUploader


    private val viewModel:
            VetProfileSetupViewModel by viewModels()


    private lateinit var toolbar:
            MaterialToolbar


    private lateinit var ivVetPhoto:
            ImageView

    private lateinit var tvAddPhoto:
            TextView


    private lateinit var etFullName:
            TextInputEditText

    private lateinit var etClinicName:
            TextInputEditText

    private lateinit var etCity:
            TextInputEditText

    private lateinit var etAddress:
            TextInputEditText

    private lateinit var etPhone:
            TextInputEditText

    private lateinit var etExperience:
            TextInputEditText


    private lateinit var actvSpecialization:
            AutoCompleteTextView


    private lateinit var btnSaveProfile:
            MaterialButton


    private lateinit var progressBar:
            CircularProgressIndicator


    private var selectedImageBytes:
            ByteArray? =
        null


    private var currentImageUrl =
        ""


    private var currentCreatedAt =
        0L


    private var currentAvailability =
        true


    private val specializations =
        listOf(
            "General Practice",
            "Surgery",
            "Dermatology",
            "Dentistry",
            "Oncology",
            "Cardiology",
            "Exotic Animals"
        )


    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (
                uri == null
            ) {

                return@registerForActivityResult
            }


            ivVetPhoto.setImageURI(
                uri
            )


            tvAddPhoto.text =
                "Change Photo"


            lifecycleScope.launch {

                selectedImageBytes =
                    readImageBytes(
                        uri
                    )


                if (
                    selectedImageBytes == null
                ) {

                    Toast.makeText(
                        this@VetProfileSetupActivity,
                        "Unable to read the selected image.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_vet_profile_setup
        )


        window.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        )


        bindViews()

        applySystemInsets()

        setupToolbar()

        setupSpecializationDropdown()

        setupClicks()

        observeUiState()

        loadExistingVetProfile()
    }


    private fun bindViews() {

        toolbar =
            findViewById(
                R.id.toolbarVetProfile
            )


        ivVetPhoto =
            findViewById(
                R.id.ivVetPhoto
            )


        tvAddPhoto =
            findViewById(
                R.id.tvAddPhoto
            )


        etFullName =
            findViewById(
                R.id.etFullName
            )


        etClinicName =
            findViewById(
                R.id.etClinicName
            )


        etCity =
            findViewById(
                R.id.etCity
            )


        etAddress =
            findViewById(
                R.id.etAddress
            )


        etPhone =
            findViewById(
                R.id.etPhone
            )


        etExperience =
            findViewById(
                R.id.etExperience
            )


        actvSpecialization =
            findViewById(
                R.id.actvSpecialization
            )


        btnSaveProfile =
            findViewById(
                R.id.btnSaveProfile
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootVetProfileSetup
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
                    0,
                    systemBars.top,
                    0,
                    systemBars.bottom
                )


                insets
            }
    }


    private fun setupToolbar() {

        setSupportActionBar(
            toolbar
        )


        supportActionBar?.apply {

            title =
                "Professional Profile"


            setDisplayHomeAsUpEnabled(
                true
            )
        }
    }


    private fun setupSpecializationDropdown() {

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_dropdown_item_1line,
                specializations
            )


        actvSpecialization
            .setAdapter(
                adapter
            )
    }


    private fun setupClicks() {

        ivVetPhoto.setOnClickListener {

            if (
                btnSaveProfile.isEnabled
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        tvAddPhoto.setOnClickListener {

            if (
                btnSaveProfile.isEnabled
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        btnSaveProfile.setOnClickListener {

            submitForm()
        }
    }


    private fun loadExistingVetProfile() {

        val uid =
            auth
                .currentUser
                ?.uid
                ?: return


        showBusy(
            true
        )


        lifecycleScope.launch {

            val result =
                vetRepository
                    .getVetById(
                        uid
                    )


            showBusy(
                false
            )


            if (
                result.isFailure
            ) {

                /*
                 * A missing vet document simply means
                 * this is the first profile setup.
                 */
                return@launch
            }


            val vet =
                result.getOrThrow()


            etFullName.setText(
                vet.displayName
            )


            etClinicName.setText(
                vet.clinicName
            )


            etCity.setText(
                vet.city
            )


            etAddress.setText(
                vet.address
            )


            etPhone.setText(
                vet.phoneNumber
            )


            etExperience.setText(
                if (
                    vet.yearsOfExperience > 0
                ) {

                    vet.yearsOfExperience
                        .toString()

                } else {

                    ""
                }
            )


            actvSpecialization
                .setText(
                    vet.specialization,
                    false
                )


            currentImageUrl =
                vet.profileImageUrl


            currentCreatedAt =
                vet.createdAt


            currentAvailability =
                vet.isAvailable


            if (
                currentImageUrl.isNotBlank()
            ) {

                ivVetPhoto.load(
                    currentImageUrl
                ) {

                    crossfade(
                        true
                    )


                    placeholder(
                        R.drawable.ic_profile
                    )


                    error(
                        R.drawable.ic_profile
                    )
                }


                tvAddPhoto.text =
                    "Change Photo"
            }


            btnSaveProfile.text =
                "Save Changes"
        }
    }


    private fun submitForm() {

        val displayName =
            etFullName
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val clinicName =
            etClinicName
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val city =
            etCity
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val address =
            etAddress
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val phone =
            etPhone
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val specialization =
            actvSpecialization
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val experience =
            etExperience
                .text
                ?.toString()
                ?.trim()
                ?.toIntOrNull()


        if (
            displayName.isBlank() ||
            clinicName.isBlank() ||
            city.isBlank() ||
            address.isBlank() ||
            phone.isBlank() ||
            specialization.isBlank()
        ) {

            Toast.makeText(
                this,
                "Please complete all professional profile fields.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        if (
            specialization !in
            specializations
        ) {

            Toast.makeText(
                this,
                "Please choose a specialization from the list.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        if (
            experience == null ||
            experience <= 0
        ) {

            Toast.makeText(
                this,
                "Enter valid years of experience.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        val uid =
            auth
                .currentUser
                ?.uid


        if (
            uid.isNullOrBlank()
        ) {

            Toast.makeText(
                this,
                "Your session has expired.",
                Toast.LENGTH_LONG
            ).show()


            return
        }


        lifecycleScope.launch {

            showBusy(
                true
            )


            if (
                selectedImageBytes != null
            ) {

                val imageResult =
                    cloudinaryUploader
                        .uploadImage(
                            imageBytes =
                                selectedImageBytes!!,

                            folder =
                                "pawconnect/vets/$uid"
                        )


                if (
                    imageResult.isFailure
                ) {

                    showBusy(
                        false
                    )


                    Toast.makeText(
                        this@VetProfileSetupActivity,
                        imageResult
                            .exceptionOrNull()
                            ?.message
                            ?: "Profile image upload failed.",
                        Toast.LENGTH_LONG
                    ).show()


                    return@launch
                }


                currentImageUrl =
                    imageResult
                        .getOrThrow()


                selectedImageBytes =
                    null
            }


            viewModel.saveVetProfile(
                uid =
                    uid,

                displayName =
                    displayName,

                clinicName =
                    clinicName,

                city =
                    city,

                address =
                    address,

                phoneNumber =
                    phone,

                specialization =
                    specialization,

                yearsOfExperience =
                    experience,

                profileImageUrl =
                    currentImageUrl,

                createdAt =
                    currentCreatedAt,

                isAvailable =
                    currentAvailability
            )
        }
    }


    private fun observeUiState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel
                    .uiState
                    .collect { state ->

                        when (state) {

                            VetProfileSetupViewModel
                                .UiState
                                .Idle -> {

                            }


                            VetProfileSetupViewModel
                                .UiState
                                .Loading -> {

                                showBusy(
                                    true
                                )
                            }


                            VetProfileSetupViewModel
                                .UiState
                                .Success -> {

                                showBusy(
                                    false
                                )


                                Toast.makeText(
                                    this@VetProfileSetupActivity,
                                    "Professional profile saved",
                                    Toast.LENGTH_SHORT
                                ).show()


                                setResult(
                                    RESULT_OK
                                )


                                /*
                                 * Return to ProfileFragment /
                                 * VetHome. Do NOT route back to
                                 * legacy DashboardActivity.
                                 */
                                finish()
                            }


                            is VetProfileSetupViewModel
                            .UiState
                            .Error -> {

                                showBusy(
                                    false
                                )


                                Toast.makeText(
                                    this@VetProfileSetupActivity,
                                    state.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            }
        }
    }


    private fun showBusy(
        busy: Boolean
    ) {

        progressBar.visibility =
            if (
                busy
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        btnSaveProfile.isEnabled =
            !busy


        etFullName.isEnabled =
            !busy


        etClinicName.isEnabled =
            !busy


        etCity.isEnabled =
            !busy


        etAddress.isEnabled =
            !busy


        etPhone.isEnabled =
            !busy


        etExperience.isEnabled =
            !busy


        actvSpecialization.isEnabled =
            !busy
    }


    private suspend fun readImageBytes(
        uri: Uri
    ): ByteArray? {

        return withContext(
            Dispatchers.IO
        ) {

            runCatching {

                contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.use { stream ->

                        stream.readBytes()
                    }

            }.getOrNull()
        }
    }


    override fun onSupportNavigateUp():
            Boolean {

        onBackPressedDispatcher
            .onBackPressed()


        return true
    }
}