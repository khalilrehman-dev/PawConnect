package com.example.authapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.authapp.R
import com.example.authapp.data.remote.CloudinaryUploader
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.ui.Vets.VetProfileSetupActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject


@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var cloudinaryUploader: CloudinaryUploader


    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvPhone: TextView

    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText

    private lateinit var btnEdit: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnVetProfessionalProfile: MaterialButton

    private lateinit var progressBar: ProgressBar

    private lateinit var layoutView: View
    private lateinit var layoutEdit: View

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvChangePhoto: TextView


    private var selectedImageBytes:
            ByteArray? = null

    private var currentImageUrl =
        ""


    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {

                ivProfileImage.setImageURI(it)

                val bitmap =
                    MediaStore.Images.Media
                        .getBitmap(
                            contentResolver,
                            it
                        )

                val stream =
                    ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    80,
                    stream
                )

                selectedImageBytes =
                    stream.toByteArray()
            }
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_profile
        )

        bindViews()

        setupToolbar()

        setupClicks()

        loadProfile()
    }


    private fun bindViews() {

        tvName =
            findViewById(
                R.id.tvName
            )

        tvEmail =
            findViewById(
                R.id.tvEmail
            )

        tvRole =
            findViewById(
                R.id.tvRole
            )

        tvPhone =
            findViewById(
                R.id.tvPhone
            )


        etName =
            findViewById(
                R.id.etName
            )

        etPhone =
            findViewById(
                R.id.etPhone
            )


        btnEdit =
            findViewById(
                R.id.btnEdit
            )

        btnSave =
            findViewById(
                R.id.btnSave
            )

        btnLogout =
            findViewById(
                R.id.btnLogout
            )

        btnVetProfessionalProfile =
            findViewById(
                R.id.btnVetProfessionalProfile
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )


        layoutView =
            findViewById(
                R.id.layoutView
            )

        layoutEdit =
            findViewById(
                R.id.layoutEdit
            )


        ivProfileImage =
            findViewById(
                R.id.ivProfileImage
            )

        tvChangePhoto =
            findViewById(
                R.id.tvChangePhoto
            )
    }


    private fun setupToolbar() {

        supportActionBar?.apply {

            title =
                "My Profile"

            setDisplayHomeAsUpEnabled(
                true
            )
        }
    }


    private fun setupClicks() {

        btnEdit.setOnClickListener {

            enterEditMode()
        }


        btnSave.setOnClickListener {

            saveProfile()
        }


        btnVetProfessionalProfile
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        VetProfileSetupActivity::class.java
                    )
                )
            }


        ivProfileImage
            .setOnClickListener {

                if (
                    layoutEdit.visibility ==
                    View.VISIBLE
                ) {

                    pickImage.launch(
                        "image/*"
                    )
                }
            }


        tvChangePhoto
            .setOnClickListener {

                pickImage.launch(
                    "image/*"
                )
            }


        btnLogout.setOnClickListener {

            showLogoutConfirmation()
        }
    }


    private fun enterEditMode() {

        layoutView.visibility =
            View.GONE

        layoutEdit.visibility =
            View.VISIBLE

        btnEdit.visibility =
            View.GONE

        btnSave.visibility =
            View.VISIBLE

        tvChangePhoto.visibility =
            View.VISIBLE
    }


    private fun loadProfile() {

        val uid =
            authRepository.getCurrentUid()
                ?: return


        lifecycleScope.launch {

            val result =
                authRepository
                    .getUserFromFirestore(uid)


            if (result.isFailure) {

                Toast.makeText(
                    this@ProfileActivity,
                    "Unable to load profile",
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }


            val user =
                result.getOrThrow()


            currentImageUrl =
                user.profileImageUrl


            if (
                currentImageUrl.isBlank()
            ) {

                ivProfileImage.setImageResource(
                    R.drawable.ic_profile
                )

            } else {

                ivProfileImage.load(
                    currentImageUrl
                ) {

                    crossfade(true)

                    placeholder(
                        R.drawable.ic_profile
                    )

                    error(
                        R.drawable.ic_profile
                    )
                }
            }


            tvName.text =
                user.displayName
                    .ifEmpty {
                        "Not set"
                    }


            tvEmail.text =
                user.email
                    .ifEmpty {
                        "Not set"
                    }


            tvPhone.text =
                user.phoneNumber
                    .ifEmpty {
                        "Not set"
                    }


            tvRole.text =
                when (user.role) {

                    "pet_owner" ->
                        "Pet Owner"

                    "veterinarian" ->
                        "Veterinarian"

                    else ->
                        "Unknown"
                }


            etName.setText(
                user.displayName
            )

            etPhone.setText(
                user.phoneNumber
            )


            btnVetProfessionalProfile.visibility =
                if (
                    user.role ==
                    "veterinarian"
                ) {

                    View.VISIBLE

                } else {

                    View.GONE
                }
        }
    }


    private fun saveProfile() {

        val name =
            etName.text
                .toString()
                .trim()

        val phone =
            etPhone.text
                .toString()
                .trim()


        if (name.isBlank()) {

            Toast.makeText(
                this,
                "Name cannot be empty",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val uid =
            authRepository.getCurrentUid()
                ?: return


        progressBar.visibility =
            View.VISIBLE

        btnSave.isEnabled =
            false


        lifecycleScope.launch {

            var imageUrl =
                currentImageUrl


            if (
                selectedImageBytes != null
            ) {

                val uploadResult =
                    cloudinaryUploader
                        .uploadImage(
                            imageBytes =
                                selectedImageBytes!!,

                            folder =
                                "pawconnect/users/$uid"
                        )


                if (
                    uploadResult.isSuccess
                ) {

                    imageUrl =
                        uploadResult
                            .getOrThrow()
                }
            }


            val result =
                authRepository
                    .updateUserProfile(
                        uid = uid,
                        displayName = name,
                        phone = phone,
                        profileImageUrl = imageUrl
                    )


            progressBar.visibility =
                View.GONE

            btnSave.isEnabled =
                true


            if (
                result.isSuccess
            ) {

                Toast.makeText(
                    this@ProfileActivity,
                    "Profile updated",
                    Toast.LENGTH_SHORT
                ).show()


                layoutEdit.visibility =
                    View.GONE

                layoutView.visibility =
                    View.VISIBLE


                btnEdit.visibility =
                    View.VISIBLE

                btnSave.visibility =
                    View.GONE


                tvChangePhoto.visibility =
                    View.GONE


                selectedImageBytes =
                    null


                loadProfile()

            } else {

                Toast.makeText(
                    this@ProfileActivity,
                    "Failed to update",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun showLogoutConfirmation() {

        MaterialAlertDialogBuilder(this)
            .setTitle(
                "Log out?"
            )
            .setMessage(
                "You will need to sign in again to access your PawConnect account."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Log out"
            ) { _, _ ->

                performLogout()
            }
            .show()
    }


    private fun performLogout() {

        btnLogout.isEnabled =
            false

        progressBar.visibility =
            View.VISIBLE


        lifecycleScope.launch {

            val result =
                authRepository.logout()


            if (
                result.isSuccess
            ) {

                startActivity(
                    Intent(
                        this@ProfileActivity,
                        WelcomeActivity::class.java
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )

                finish()

            } else {

                progressBar.visibility =
                    View.GONE

                btnLogout.isEnabled =
                    true


                Toast.makeText(
                    this@ProfileActivity,
                    "Unable to log out. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onSupportNavigateUp():
            Boolean {

        onBackPressedDispatcher
            .onBackPressed()

        return true
    }
}