package com.example.authapp.ui.Vets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.authapp.R
import com.example.authapp.presentation.vets.VetProfileSetupViewModel
import com.example.authapp.ui.DashboardActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VetProfileSetupActivity : AppCompatActivity() {

    private val viewModel: VetProfileSetupViewModel by viewModels()

    @Inject lateinit var auth: FirebaseAuth

    private lateinit var etFullName: TextInputEditText
    private lateinit var etClinicName: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etExperience: TextInputEditText
    private lateinit var actvSpecialization: AutoCompleteTextView
    private lateinit var btnSaveProfile: Button
    private lateinit var progressBar: ProgressBar

    private val specializations = listOf(
        "General Practice", "Surgery", "Dermatology",
        "Dentistry", "Oncology", "Cardiology", "Exotic Animals"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_profile_setup)

        etFullName       = findViewById(R.id.etFullName)
        etClinicName     = findViewById(R.id.etClinicName)
        etCity           = findViewById(R.id.etCity)
        etAddress        = findViewById(R.id.etAddress)
        etPhone          = findViewById(R.id.etPhone)
        etExperience     = findViewById(R.id.etExperience)
        actvSpecialization = findViewById(R.id.actvSpecialization)
        btnSaveProfile   = findViewById(R.id.btnSaveProfile)
        progressBar      = findViewById(R.id.progressBar)

        supportActionBar?.apply {
            title = "Setup Vet Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        setupSpecializationDropdown()
        observeUiState()

        btnSaveProfile.setOnClickListener { submitForm() }
    }

    private fun setupSpecializationDropdown() {
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, specializations
        )
        actvSpecialization.setAdapter(adapter)
    }

    private fun submitForm() {
        val displayName    = etFullName.text.toString().trim()
        val clinicName     = etClinicName.text.toString().trim()
        val city           = etCity.text.toString().trim()
        val address        = etAddress.text.toString().trim()
        val phone          = etPhone.text.toString().trim()
        val specialization = actvSpecialization.text.toString().trim()
        val experience     = etExperience.text.toString().trim().toIntOrNull() ?: 0

        if (displayName.isBlank() || clinicName.isBlank() || city.isBlank() ||
            address.isBlank() || phone.isBlank() || specialization.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        viewModel.saveVetProfile(
            uid               = uid,
            displayName       = displayName,
            clinicName        = clinicName,
            city              = city,
            address           = address,
            phoneNumber       = phone,
            specialization    = specialization,
            yearsOfExperience = experience,
            profileImageUrl   = ""
        )
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
                            Toast.makeText(this@VetProfileSetupActivity, "Profile saved!", Toast.LENGTH_SHORT).show()
                            startActivity(
                                Intent(
                                    this@VetProfileSetupActivity,
                                    DashboardActivity::class.java
                                ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                        is VetProfileSetupViewModel.UiState.Error -> {
                            progressBar.visibility = View.GONE
                            btnSaveProfile.isEnabled = true
                            Toast.makeText(this@VetProfileSetupActivity, state.message, Toast.LENGTH_LONG).show()
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