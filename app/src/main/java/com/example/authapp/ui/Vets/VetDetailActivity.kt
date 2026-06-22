package com.example.authapp.ui.Vets

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.example.authapp.R
import com.example.authapp.presentation.vets.FindVetsViewModel
import com.example.authapp.ui.Appointments.BookAppointmentActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VetDetailActivity : AppCompatActivity() {

    private val viewModel: FindVetsViewModel by viewModels()

    private lateinit var ivVetPhoto: ImageView
    private lateinit var tvVetName: TextView
    private lateinit var tvSpecialization: TextView
    private lateinit var tvClinicName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvExperience: TextView
    private lateinit var btnCall: Button
    private lateinit var btnMessage: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_detail)

        ivVetPhoto       = findViewById(R.id.ivVetPhoto)
        tvVetName        = findViewById(R.id.tvVetName)
        tvSpecialization = findViewById(R.id.tvSpecialization)
        tvClinicName     = findViewById(R.id.tvClinicName)
        tvAddress        = findViewById(R.id.tvAddress)
        tvCity           = findViewById(R.id.tvCity)
        tvExperience     = findViewById(R.id.tvExperience)
        btnCall          = findViewById(R.id.btnCall)
        btnMessage       = findViewById(R.id.btnMessage)
        progressBar      = findViewById(R.id.progressBar)

        supportActionBar?.apply {
            title = "Vet Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        val vetUid = intent.getStringExtra("VET_UID") ?: run { finish(); return }

        viewModel.loadAllVets()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is FindVetsViewModel.UiState.Success) {
                        val vet = viewModel.getVetById(vetUid) ?: return@collect
                        progressBar.visibility = View.GONE

                        tvVetName.text        = "Dr. ${vet.displayName}"
                        tvSpecialization.text = vet.specialization
                        tvClinicName.text     = vet.clinicName
                        tvAddress.text        = vet.address
                        tvCity.text           = vet.city
                        tvExperience.text     = "${vet.yearsOfExperience} years of experience"

                        ivVetPhoto.load(vet.profileImageUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_pet_placeholder)
                            error(R.drawable.ic_pet_placeholder)
                            transformations(CircleCropTransformation())
                        }

                        btnCall.setOnClickListener {
                            startActivity(Intent(this@VetDetailActivity, BookAppointmentActivity::class.java).apply {
                                putExtra("vetId", vet.uid)
                                putExtra("vetName", vet.displayName)
                                putExtra("clinic", vet.clinicName)
                            })
                        }

                        btnMessage.setOnClickListener {
                            Toast.makeText(this@VetDetailActivity, "Chat coming soon", Toast.LENGTH_SHORT).show()
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