package com.example.authapp.ui.pets

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.authapp.R
import com.example.authapp.presentation.pets.PetActionState
import com.example.authapp.presentation.pets.PetEvent
import com.example.authapp.presentation.pets.PetViewModel
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class EditPetActivity : AppCompatActivity() {

    private val viewModel: PetViewModel by viewModels()

    private lateinit var ivPetPhoto: ImageView
    private lateinit var tvChangePhoto: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etBreed: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerGender: Spinner
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    // Data passed from PetDetailActivity
    private var petId: String        = ""
    private var ownerId: String      = ""
    private var existingImageUrl: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            ivPetPhoto.setImageURI(it)
            tvChangePhoto.text = "Change Photo"
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            viewModel.setImage(stream.toByteArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_pet)

        // Read all pet data from intent
        petId            = intent.getStringExtra("petId")      ?: ""
        ownerId          = intent.getStringExtra("ownerId")    ?: ""
        existingImageUrl = intent.getStringExtra("petImage")   ?: ""

        bindViews()
        setupSpinners()
        prefillData()
        observeViewModel()

        ivPetPhoto.setOnClickListener    { pickImage.launch("image/*") }
        tvChangePhoto.setOnClickListener { pickImage.launch("image/*") }

        btnSave.setOnClickListener {
            viewModel.editPet(
                petId            = petId,
                ownerId          = ownerId,
                name             = etName.text.toString(),
                species          = spinnerSpecies.selectedItem.toString(),
                breed            = etBreed.text.toString(),
                age              = etAge.text.toString(),
                gender           = spinnerGender.selectedItem.toString(),
                description      = etDescription.text.toString(),
                existingImageUrl = existingImageUrl
            )
        }

        supportActionBar?.apply {
            title = "Edit Pet"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun bindViews() {
        ivPetPhoto    = findViewById(R.id.ivPetPhoto)
        tvChangePhoto = findViewById(R.id.tvAddPhoto)
        etName        = findViewById(R.id.etName)
        etBreed       = findViewById(R.id.etBreed)
        etAge         = findViewById(R.id.etAge)
        etDescription = findViewById(R.id.etDescription)
        spinnerSpecies = findViewById(R.id.spinnerSpecies)
        spinnerGender  = findViewById(R.id.spinnerGender)
        btnSave        = findViewById(R.id.btnSave)
        progressBar    = findViewById(R.id.progressBar)
    }

    private fun setupSpinners() {
        val species = listOf("Select Species", "Dog", "Cat", "Bird", "Rabbit", "Other")
        spinnerSpecies.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, species)

        val genders = listOf("Select Gender", "Male", "Female")
        spinnerGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
    }

    private fun prefillData() {
        // Pre-fill all fields with existing pet data
        etName.setText(intent.getStringExtra("petName") ?: "")
        etBreed.setText(intent.getStringExtra("petBreed") ?: "")
        etAge.setText(intent.getIntExtra("petAge", 0).toString())
        etDescription.setText(intent.getStringExtra("petDesc") ?: "")

        // Set spinner selections
        val speciesList = listOf("Select Species", "Dog", "Cat", "Bird", "Rabbit", "Other")
        val savedSpecies = intent.getStringExtra("petSpecies") ?: ""
        spinnerSpecies.setSelection(speciesList.indexOf(savedSpecies).takeIf { it >= 0 } ?: 0)

        val genderList = listOf("Select Gender", "Male", "Female")
        val savedGender = intent.getStringExtra("petGender") ?: ""
        spinnerGender.setSelection(genderList.indexOf(savedGender).takeIf { it >= 0 } ?: 0)

        // Load existing image
        if (existingImageUrl.isNotEmpty()) {
            ivPetPhoto.visibility = View.VISIBLE
            ivPetPhoto.load(existingImageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_pet_placeholder)
            }
            tvChangePhoto.text = "Change Photo"
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is PetActionState.Idle    -> showLoading(false)
                            is PetActionState.Loading -> showLoading(true)
                            is PetActionState.Success -> showLoading(false)
                            is PetActionState.Error   -> {
                                showLoading(false)
                                Toast.makeText(this@EditPetActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is PetEvent.NavigateBack -> finish()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled      = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}