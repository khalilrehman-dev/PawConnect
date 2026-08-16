package com.example.authapp.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.authapp.R
import com.example.authapp.model.User
import com.example.authapp.presentation.profile.ProfileEvent
import com.example.authapp.presentation.profile.ProfileUiState
import com.example.authapp.presentation.profile.ProfileViewModel
import com.example.authapp.ui.Vets.VetProfileSetupActivity
import com.example.authapp.ui.WelcomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class ProfileFragment :
    Fragment(R.layout.fragment_profile) {

    private val viewModel:
            ProfileViewModel by viewModels()


    private lateinit var profileContent:
            View


    private lateinit var layoutLoading:
            View

    private lateinit var layoutError:
            View

    private lateinit var tvError:
            TextView


    private lateinit var ivProfileImage:
            ImageView

    private lateinit var tvChangePhoto:
            TextView


    private lateinit var tvName:
            TextView

    private lateinit var tvRole:
            TextView

    private lateinit var tvEmail:
            TextView

    private lateinit var tvPhone:
            TextView


    private lateinit var layoutView:
            View

    private lateinit var layoutEdit:
            View


    private lateinit var etName:
            TextInputEditText

    private lateinit var etPhone:
            TextInputEditText


    private lateinit var btnProfessionalProfile:
            MaterialButton

    private lateinit var btnEdit:
            MaterialButton

    private lateinit var btnSave:
            MaterialButton

    private lateinit var btnCancelEdit:
            MaterialButton

    private lateinit var btnLogout:
            MaterialButton

    private lateinit var btnRetry:
            MaterialButton


    private var selectedImageBytes:
            ByteArray? =
        null


    private var isEditing =
        false


    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (
                uri == null
            ) {

                return@registerForActivityResult
            }


            ivProfileImage.setImageURI(
                uri
            )


            viewLifecycleOwner
                .lifecycleScope
                .launch {

                    selectedImageBytes =
                        readImageBytes(
                            uri
                        )


                    if (
                        selectedImageBytes == null
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Unable to read the selected image.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        bindViews(
            view
        )

        setupClicks()

        observeViewModel()
    }


    override fun onResume() {
        super.onResume()


        /*
         * Also refreshes the profile after returning
         * from Professional Profile editing.
         */
        if (
            !isEditing
        ) {

            viewModel.loadProfile()
        }
    }


    private fun bindViews(
        view: View
    ) {

        profileContent =
            view.findViewById(
                R.id.profileContent
            )


        layoutLoading =
            view.findViewById(
                R.id.layoutProfileLoading
            )


        layoutError =
            view.findViewById(
                R.id.layoutProfileError
            )


        tvError =
            view.findViewById(
                R.id.tvProfileError
            )


        ivProfileImage =
            view.findViewById(
                R.id.ivProfileImage
            )


        tvChangePhoto =
            view.findViewById(
                R.id.tvChangePhoto
            )


        tvName =
            view.findViewById(
                R.id.tvName
            )


        tvRole =
            view.findViewById(
                R.id.tvRole
            )


        tvEmail =
            view.findViewById(
                R.id.tvEmail
            )


        tvPhone =
            view.findViewById(
                R.id.tvPhone
            )


        layoutView =
            view.findViewById(
                R.id.layoutView
            )


        layoutEdit =
            view.findViewById(
                R.id.layoutEdit
            )


        etName =
            view.findViewById(
                R.id.etName
            )


        etPhone =
            view.findViewById(
                R.id.etPhone
            )


        btnProfessionalProfile =
            view.findViewById(
                R.id.btnVetProfessionalProfile
            )


        btnEdit =
            view.findViewById(
                R.id.btnEdit
            )


        btnSave =
            view.findViewById(
                R.id.btnSave
            )


        btnCancelEdit =
            view.findViewById(
                R.id.btnCancelEdit
            )


        btnLogout =
            view.findViewById(
                R.id.btnLogout
            )


        btnRetry =
            view.findViewById(
                R.id.btnRetryProfile
            )
    }


    private fun setupClicks() {

        btnEdit.setOnClickListener {

            enterEditMode()
        }


        btnCancelEdit.setOnClickListener {

            leaveEditMode(
                restoreProfile =
                    true
            )
        }


        btnSave.setOnClickListener {

            viewModel.saveProfile(
                displayName =
                    etName
                        .text
                        ?.toString()
                        .orEmpty(),

                phone =
                    etPhone
                        .text
                        ?.toString()
                        .orEmpty(),

                imageBytes =
                    selectedImageBytes
            )
        }


        ivProfileImage.setOnClickListener {

            if (
                isEditing
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        tvChangePhoto.setOnClickListener {

            if (
                isEditing
            ) {

                pickImage.launch(
                    "image/*"
                )
            }
        }


        btnProfessionalProfile
            .setOnClickListener {

                startActivity(
                    Intent(
                        requireContext(),
                        VetProfileSetupActivity::class.java
                    )
                )
            }


        btnLogout.setOnClickListener {

            showLogoutConfirmation()
        }


        btnRetry.setOnClickListener {

            viewModel.loadProfile()
        }
    }


    private fun observeViewModel() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        launch {

                            viewModel
                                .uiState
                                .collect { state ->

                                    when (state) {

                                        ProfileUiState.Loading -> {

                                            showLoading()
                                        }


                                        is ProfileUiState.Success -> {

                                            showProfile(
                                                state
                                            )
                                        }


                                        is ProfileUiState.Error -> {

                                            showError(
                                                state.message
                                            )
                                        }
                                    }
                                }
                        }


                        launch {

                            viewModel
                                .events
                                .collect { event ->

                                    when (event) {

                                        ProfileEvent.ProfileSaved -> {

                                            Toast.makeText(
                                                requireContext(),
                                                "Profile updated",
                                                Toast.LENGTH_SHORT
                                            ).show()


                                            selectedImageBytes =
                                                null


                                            leaveEditMode(
                                                restoreProfile =
                                                    false
                                            )
                                        }


                                        ProfileEvent.LoggedOut -> {

                                            startActivity(
                                                Intent(
                                                    requireContext(),
                                                    WelcomeActivity::class.java
                                                ).apply {

                                                    flags =
                                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                }
                                            )


                                            requireActivity()
                                                .finish()
                                        }


                                        is ProfileEvent.Error -> {

                                            Toast.makeText(
                                                requireContext(),
                                                event.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                        }
                    }
            }
    }


    private fun showLoading() {

        layoutLoading.visibility =
            View.VISIBLE


        profileContent.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE
    }


    private fun showProfile(
        state: ProfileUiState.Success
    ) {

        layoutLoading.visibility =
            View.GONE


        layoutError.visibility =
            View.GONE


        profileContent.visibility =
            View.VISIBLE


        renderUser(
            state.user
        )


        val busy =
            state.isSaving ||
                    state.isLoggingOut


        btnSave.isEnabled =
            !busy


        btnCancelEdit.isEnabled =
            !busy


        btnEdit.isEnabled =
            !busy


        btnLogout.isEnabled =
            !busy


        btnProfessionalProfile.isEnabled =
            !busy


        etName.isEnabled =
            !busy


        etPhone.isEnabled =
            !busy


        btnSave.text =
            if (
                state.isSaving
            ) {

                "Saving..."

            } else {

                "Save Changes"
            }


        btnLogout.text =
            if (
                state.isLoggingOut
            ) {

                "Signing Out..."

            } else {

                "Sign Out"
            }
    }


    private fun renderUser(
        user: User
    ) {

        tvName.text =
            user.displayName
                .ifBlank {

                    "PawConnect User"
                }


        tvRole.text =
            when (
                user.role
            ) {

                "veterinarian" ->
                    "Veterinarian"

                "pet_owner" ->
                    "Pet Owner"

                else ->
                    "PawConnect Member"
            }


        tvEmail.text =
            user.email
                .ifBlank {

                    "Not provided"
                }


        tvPhone.text =
            user.phoneNumber
                .ifBlank {

                    "Not provided"
                }


        /*
         * Don't overwrite a newly selected local
         * preview while editing.
         */
        if (
            !isEditing ||
            selectedImageBytes == null
        ) {

            if (
                user.profileImageUrl
                    .isBlank()
            ) {

                ivProfileImage.setImageResource(
                    R.drawable.ic_profile
                )

            } else {

                ivProfileImage.load(
                    user.profileImageUrl
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
            }
        }


        if (
            !isEditing
        ) {

            etName.setText(
                user.displayName
            )


            etPhone.setText(
                user.phoneNumber
            )
        }


        btnProfessionalProfile.visibility =
            if (
                user.role ==
                "veterinarian"
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }
    }


    private fun enterEditMode() {

        isEditing =
            true


        selectedImageBytes =
            null


        layoutView.visibility =
            View.GONE


        layoutEdit.visibility =
            View.VISIBLE


        btnEdit.visibility =
            View.GONE


        btnSave.visibility =
            View.VISIBLE


        btnCancelEdit.visibility =
            View.VISIBLE


        tvChangePhoto.visibility =
            View.VISIBLE
    }


    private fun leaveEditMode(
        restoreProfile: Boolean
    ) {

        isEditing =
            false


        selectedImageBytes =
            null


        layoutEdit.visibility =
            View.GONE


        layoutView.visibility =
            View.VISIBLE


        btnEdit.visibility =
            View.VISIBLE


        btnSave.visibility =
            View.GONE


        btnCancelEdit.visibility =
            View.GONE


        tvChangePhoto.visibility =
            View.GONE


        if (
            restoreProfile
        ) {

            val state =
                viewModel.uiState.value


            if (
                state is ProfileUiState.Success
            ) {

                renderUser(
                    state.user
                )
            }
        }
    }


    private fun showError(
        message: String
    ) {

        layoutLoading.visibility =
            View.GONE


        profileContent.visibility =
            View.GONE


        tvError.text =
            message


        layoutError.visibility =
            View.VISIBLE
    }


    private fun showLogoutConfirmation() {

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                "Sign out?"
            )
            .setMessage(
                "You will need to sign in again to continue using PawConnect."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Sign Out"
            ) { _, _ ->

                viewModel.logout()
            }
            .show()
    }


    private suspend fun readImageBytes(
        uri: Uri
    ): ByteArray? {

        return withContext(
            Dispatchers.IO
        ) {

            runCatching {

                requireContext()
                    .contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.use { stream ->

                        stream.readBytes()
                    }

            }.getOrNull()
        }
    }
}