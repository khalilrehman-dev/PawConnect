package com.example.authapp.ui.pets

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
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
import com.example.authapp.model.Pet
import com.example.authapp.presentation.pets.PetActionState
import com.example.authapp.presentation.pets.PetDetailUiState
import com.example.authapp.presentation.pets.PetEvent
import com.example.authapp.presentation.pets.PetViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class EditPetActivity :
    AppCompatActivity() {

    private val viewModel:
            PetViewModel by viewModels()


    private lateinit var toolbar:
            MaterialToolbar


    private lateinit var content:
            View

    private lateinit var layoutLoading:
            View

    private lateinit var layoutError:
            View

    private lateinit var tvError:
            TextView

    private lateinit var btnRetry:
            MaterialButton


    private lateinit var ivPetPhoto:
            ImageView

    private lateinit var tvChangePhoto:
            TextView


    private lateinit var etName:
            TextInputEditText

    private lateinit var etBreed:
            TextInputEditText

    private lateinit var etAge:
            TextInputEditText

    private lateinit var etDescription:
            TextInputEditText


    private lateinit var spinnerSpecies:
            Spinner

    private lateinit var spinnerGender:
            Spinner


    private lateinit var btnSave:
            MaterialButton

    private lateinit var progressBar:
            CircularProgressIndicator


    private var petId =
        ""


    private var currentPet:
            Pet? =
        null


    private var isSaving =
        false

    private var isReadingImage =
        false


    private val speciesOptions =
        listOf(
            "Select Species",
            "Dog",
            "Cat",
            "Bird",
            "Rabbit",
            "Other"
        )


    private val genderOptions =
        listOf(
            "Select Gender",
            "Male",
            "Female"
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


            handleSelectedImage(
                uri
            )
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        setContentView(
            R.layout.activity_edit_pet
        )


        window.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE
        )


        petId =
            intent
                .getStringExtra(
                    "petId"
                )
                .orEmpty()


        bindViews()

        applySystemInsets()

        setupToolbar()

        setupSpinners()

        setupClicks()

        observeViewModel()


        if (
            petId.isBlank()
        ) {

            showLoadError(
                "Pet information is missing.",
                canRetry = false
            )

        } else {

            viewModel.loadPetForEdit(
                petId
            )
        }
    }


    private fun bindViews() {

        toolbar =
            findViewById(
                R.id.toolbarEditPet
            )


        content =
            findViewById(
                R.id.contentEditPet
            )


        layoutLoading =
            findViewById(
                R.id.layoutEditPetLoading
            )


        layoutError =
            findViewById(
                R.id.layoutEditPetError
            )


        tvError =
            findViewById(
                R.id.tvEditPetError
            )


        btnRetry =
            findViewById(
                R.id.btnRetryEditPet
            )


        ivPetPhoto =
            findViewById(
                R.id.ivPetPhoto
            )


        tvChangePhoto =
            findViewById(
                R.id.tvAddPhoto
            )


        etName =
            findViewById(
                R.id.etName
            )


        etBreed =
            findViewById(
                R.id.etBreed
            )


        etAge =
            findViewById(
                R.id.etAge
            )


        etDescription =
            findViewById(
                R.id.etDescription
            )


        spinnerSpecies =
            findViewById(
                R.id.spinnerSpecies
            )


        spinnerGender =
            findViewById(
                R.id.spinnerGender
            )


        btnSave =
            findViewById(
                R.id.btnSave
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootEditPet
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
                "Edit Pet"


            setDisplayHomeAsUpEnabled(
                true
            )
        }
    }


    private fun setupSpinners() {

        spinnerSpecies.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                speciesOptions
            )


        spinnerGender.adapter =
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_spinner_dropdown_item,
                genderOptions
            )
    }


    private fun setupClicks() {

        btnRetry.setOnClickListener {

            if (
                petId.isNotBlank()
            ) {

                viewModel.loadPetForEdit(
                    petId
                )
            }
        }


        ivPetPhoto.setOnClickListener {

            if (
                canInteract()
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        tvChangePhoto.setOnClickListener {

            if (
                canInteract()
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        btnSave.setOnClickListener {

            if (
                currentPet == null
            ) {

                return@setOnClickListener
            }


            viewModel.editPet(
                petId =
                    petId,

                name =
                    etName
                        .text
                        ?.toString()
                        .orEmpty(),

                species =
                    spinnerSpecies
                        .selectedItem
                        ?.toString()
                        .orEmpty(),

                breed =
                    etBreed
                        .text
                        ?.toString()
                        .orEmpty(),

                age =
                    etAge
                        .text
                        ?.toString()
                        .orEmpty(),

                gender =
                    spinnerGender
                        .selectedItem
                        ?.toString()
                        .orEmpty(),

                description =
                    etDescription
                        .text
                        ?.toString()
                        .orEmpty()
            )
        }
    }


    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel
                        .detailState
                        .collect { state ->

                            when (state) {

                                PetDetailUiState.Idle -> {
                                    // Initial loader is in XML.
                                }


                                PetDetailUiState.Loading -> {

                                    showInitialLoading()
                                }


                                is PetDetailUiState.Success -> {

                                    currentPet =
                                        state.pet


                                    prefillPet(
                                        state.pet
                                    )


                                    showLoadedContent()
                                }


                                is PetDetailUiState.Error -> {

                                    showLoadError(
                                        state.message,
                                        canRetry =
                                            petId.isNotBlank()
                                    )
                                }
                            }
                        }
                }


                launch {

                    viewModel
                        .actionState
                        .collect { state ->

                            when (state) {

                                PetActionState.Idle -> {

                                    showSaving(
                                        false
                                    )
                                }


                                PetActionState.Loading -> {

                                    showSaving(
                                        true
                                    )
                                }


                                PetActionState.Success -> {

                                    showSaving(
                                        false
                                    )
                                }


                                is PetActionState.Error -> {

                                    showSaving(
                                        false
                                    )


                                    Toast.makeText(
                                        this@EditPetActivity,
                                        state.message,
                                        Toast.LENGTH_LONG
                                    ).show()


                                    viewModel
                                        .clearActionState()
                                }
                            }
                        }
                }


                launch {

                    viewModel
                        .events
                        .collect { event ->

                            when (event) {

                                PetEvent.NavigateBack -> {

                                    finish()
                                }
                            }
                        }
                }
            }
        }
    }


    private fun prefillPet(
        pet: Pet
    ) {

        etName.setText(
            pet.name
        )


        etBreed.setText(
            pet.breed
        )


        etAge.setText(
            pet.age
                .toString()
        )


        etDescription.setText(
            pet.description
        )


        val speciesIndex =
            speciesOptions
                .indexOf(
                    pet.species
                )


        spinnerSpecies.setSelection(
            if (
                speciesIndex >= 0
            ) {

                speciesIndex

            } else {

                0
            }
        )


        val genderIndex =
            genderOptions
                .indexOf(
                    pet.gender
                )


        spinnerGender.setSelection(
            if (
                genderIndex >= 0
            ) {

                genderIndex

            } else {

                0
            }
        )


        if (
            pet.imageUrl.isBlank()
        ) {

            ivPetPhoto.setImageResource(
                R.drawable.ic_pet_placeholder
            )

            tvChangePhoto.text =
                "Add Photo"

        } else {

            ivPetPhoto.load(
                pet.imageUrl
            ) {

                crossfade(
                    true
                )


                placeholder(
                    R.drawable.ic_pet_placeholder
                )


                error(
                    R.drawable.ic_pet_placeholder
                )
            }


            tvChangePhoto.text =
                "Change Photo"
        }
    }


    private fun handleSelectedImage(
        uri: Uri
    ) {

        ivPetPhoto.setImageURI(
            uri
        )


        isReadingImage =
            true


        tvChangePhoto.text =
            "Preparing photo..."


        updateFormEnabled()


        lifecycleScope.launch {

            val bytes =
                readImageBytes(
                    uri
                )


            if (
                bytes == null ||
                bytes.isEmpty()
            ) {

                val pet =
                    currentPet


                if (
                    pet != null &&
                    pet.imageUrl.isNotBlank()
                ) {

                    ivPetPhoto.load(
                        pet.imageUrl
                    ) {

                        placeholder(
                            R.drawable.ic_pet_placeholder
                        )


                        error(
                            R.drawable.ic_pet_placeholder
                        )
                    }

                } else {

                    ivPetPhoto.setImageResource(
                        R.drawable.ic_pet_placeholder
                    )
                }


                tvChangePhoto.text =
                    "Change Photo"


                Toast.makeText(
                    this@EditPetActivity,
                    "Unable to read the selected image.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                viewModel.setImage(
                    bytes
                )


                tvChangePhoto.text =
                    "Change Photo"
            }


            isReadingImage =
                false


            updateFormEnabled()
        }
    }


    private fun showInitialLoading() {

        content.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        layoutLoading.visibility =
            View.VISIBLE
    }


    private fun showLoadedContent() {

        layoutLoading.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        content.visibility =
            View.VISIBLE


        updateFormEnabled()
    }


    private fun showLoadError(
        message: String,
        canRetry: Boolean
    ) {

        content.visibility =
            View.GONE


        layoutLoading.visibility =
            View.GONE


        tvError.text =
            message


        btnRetry.visibility =
            if (
                canRetry
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        layoutError.visibility =
            View.VISIBLE
    }


    private fun showSaving(
        saving: Boolean
    ) {

        isSaving =
            saving


        progressBar.visibility =
            if (
                saving
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        btnSave.text =
            if (
                saving
            ) {

                "Saving Changes..."

            } else {

                "Save Changes"
            }


        updateFormEnabled()
    }


    private fun canInteract():
            Boolean {

        return currentPet != null &&
                !isSaving &&
                !isReadingImage
    }


    private fun updateFormEnabled() {

        val enabled =
            canInteract()


        btnSave.isEnabled =
            enabled


        etName.isEnabled =
            enabled


        etBreed.isEnabled =
            enabled


        etAge.isEnabled =
            enabled


        etDescription.isEnabled =
            enabled


        spinnerSpecies.isEnabled =
            enabled


        spinnerGender.isEnabled =
            enabled


        ivPetPhoto.isEnabled =
            enabled


        tvChangePhoto.isEnabled =
            enabled
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