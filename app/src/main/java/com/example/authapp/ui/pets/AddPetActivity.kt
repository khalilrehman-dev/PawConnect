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
import com.example.authapp.R
import com.example.authapp.presentation.pets.PetActionState
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
class AddPetActivity :
    AppCompatActivity() {

    private val viewModel:
            PetViewModel by viewModels()


    private lateinit var toolbar:
            MaterialToolbar


    private lateinit var ivPetPhoto:
            ImageView

    private lateinit var tvAddPhoto:
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
            R.layout.activity_add_pet
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

        setupSpinners()

        setupClicks()

        observeViewModel()
    }


    private fun bindViews() {

        toolbar =
            findViewById(
                R.id.toolbarAddPet
            )


        ivPetPhoto =
            findViewById(
                R.id.ivPetPhoto
            )


        tvAddPhoto =
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
                R.id.rootAddPet
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
                "Add Pet"


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

        ivPetPhoto.setOnClickListener {

            if (
                !isSaving &&
                !isReadingImage
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        tvAddPhoto.setOnClickListener {

            if (
                !isSaving &&
                !isReadingImage
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        btnSave.setOnClickListener {

            viewModel.addPet(
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


    private fun handleSelectedImage(
        uri: Uri
    ) {

        ivPetPhoto.setImageURI(
            uri
        )


        isReadingImage =
            true


        tvAddPhoto.text =
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

                ivPetPhoto.setImageResource(
                    R.drawable.ic_pet_placeholder
                )


                tvAddPhoto.text =
                    "Add Pet Photo"


                Toast.makeText(
                    this@AddPetActivity,
                    "Unable to read the selected image.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                viewModel.setImage(
                    bytes
                )


                tvAddPhoto.text =
                    "Change Photo"
            }


            isReadingImage =
                false


            updateFormEnabled()
        }
    }


    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

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
                                        this@AddPetActivity,
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

                "Saving Pet..."

            } else {

                "Save Pet"
            }


        updateFormEnabled()
    }


    private fun updateFormEnabled() {

        val enabled =
            !isSaving &&
                    !isReadingImage


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


        tvAddPhoto.isEnabled =
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