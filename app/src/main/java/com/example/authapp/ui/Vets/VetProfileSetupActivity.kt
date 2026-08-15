package com.example.authapp.ui.Vets

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.authapp.R
import com.example.authapp.data.remote.CloudinaryUploader
import com.example.authapp.presentation.vets.VetProfileSetupViewModel
import com.example.authapp.ui.DashboardActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

import coil.load
import com.example.authapp.domain.repository.VetRepository

@AndroidEntryPoint
class VetProfileSetupActivity : AppCompatActivity() {
    @Inject
    lateinit var vetRepository: VetRepository
    private val viewModel: VetProfileSetupViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var cloudinaryUploader: CloudinaryUploader

    private lateinit var etFullName: TextInputEditText
    private lateinit var etClinicName: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etExperience: TextInputEditText
    private lateinit var actvSpecialization: AutoCompleteTextView
    private lateinit var btnSaveProfile: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageBytes: ByteArray? = null

    private var currentImageUrl = ""
    private var currentCreatedAt = 0L

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                findViewById<ImageView>(R.id.ivVetPhoto).setImageURI(it)
                findViewById<TextView>(R.id.tvAddPhoto).text = "Change Photo"

                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                selectedImageBytes = stream.toByteArray()
            }
        }

    private val specializations = listOf(
        "General Practice",
        "Surgery",
        "Dermatology",
        "Dentistry",
        "Oncology",
        "Cardiology",
        "Exotic Animals"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_profile_setup)

        etFullName = findViewById(R.id.etFullName)
        etClinicName = findViewById(R.id.etClinicName)
        etCity = findViewById(R.id.etCity)
        etAddress = findViewById(R.id.etAddress)
        etPhone = findViewById(R.id.etPhone)
        etExperience = findViewById(R.id.etExperience)
        actvSpecialization = findViewById(R.id.actvSpecialization)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        progressBar = findViewById(R.id.progressBar)
        setupSpecializationDropdown()
        loadExistingVetProfile()


        supportActionBar?.apply {
            title = "Setup Vet Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        setupSpecializationDropdown()

        findViewById<ImageView>(R.id.ivVetPhoto).setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<TextView>(R.id.tvAddPhoto).setOnClickListener {
            pickImage.launch("image/*")
        }

        observeUiState()

        btnSaveProfile.setOnClickListener {
            submitForm()
        }

    }
    private fun loadExistingVetProfile() {

        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {

            val result =
                vetRepository.getVetById(uid)

            if (result.isSuccess) {

                val vet = result.getOrThrow()

                etFullName.setText(vet.displayName)
                etClinicName.setText(vet.clinicName)
                etCity.setText(vet.city)
                etAddress.setText(vet.address)
                etPhone.setText(vet.phoneNumber)

                etExperience.setText(
                    vet.yearsOfExperience.toString()
                )

                actvSpecialization.setText(
                    vet.specialization,
                    false
                )

                currentImageUrl =
                    vet.profileImageUrl

                currentCreatedAt =
                    vet.createdAt

                if (currentImageUrl.isNotBlank()) {

                    findViewById<ImageView>(
                        R.id.ivVetPhoto
                    ).load(currentImageUrl)

                    findViewById<TextView>(
                        R.id.tvAddPhoto
                    ).text = "Change Photo"
                }

                btnSaveProfile.text =
                    "Save Changes"
            }
        }
    }
    private fun setupSpecializationDropdown() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            specializations
        )
        actvSpecialization.setAdapter(adapter)
    }



    private fun submitForm() {

        val displayName = etFullName.text.toString().trim()
        val clinicName = etClinicName.text.toString().trim()
        val city = etCity.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val specialization = actvSpecialization.text.toString().trim()
        val experience = etExperience.text.toString().trim().toIntOrNull() ?: 0

        if (displayName.isBlank() ||
            clinicName.isBlank() ||
            city.isBlank() ||
            address.isBlank() ||
            phone.isBlank() ||
            specialization.isBlank()
        ) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {

            progressBar.visibility = View.VISIBLE
            btnSaveProfile.isEnabled = false

            var imageUrl = currentImageUrl

            if (selectedImageBytes != null) {

                val imageResult =
                    cloudinaryUploader.uploadImage(
                        imageBytes = selectedImageBytes!!,
                        folder = "pawconnect/vets/$uid"
                    )

                if (imageResult.isFailure) {

                    progressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true

                    Toast.makeText(
                        this@VetProfileSetupActivity,
                        "Profile image upload failed",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                imageUrl =
                    imageResult.getOrThrow()
            }

            viewModel.saveVetProfile(
                uid = uid,
                displayName = displayName,
                clinicName = clinicName,
                city = city,
                address = address,
                phoneNumber = phone,
                specialization = specialization,
                yearsOfExperience = experience,
                profileImageUrl = imageUrl,
                createdAt = currentCreatedAt
            )
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {

                        is VetProfileSetupViewModel.UiState.Idle -> Unit

                        is VetProfileSetupViewModel.UiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            btnSaveProfile.isEnabled = false
                        }

                        is VetProfileSetupViewModel.UiState.Success -> {
                            progressBar.visibility = View.GONE
                            btnSaveProfile.isEnabled = true

                            Toast.makeText(
                                this@VetProfileSetupActivity,
                                "Profile saved!",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(
                                    this@VetProfileSetupActivity,
                                    DashboardActivity::class.java
                                ).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }

                        is VetProfileSetupViewModel.UiState.Error -> {
                            progressBar.visibility = View.GONE
                            btnSaveProfile.isEnabled = true

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}