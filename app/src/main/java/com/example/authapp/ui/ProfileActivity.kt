package com.example.authapp.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.authapp.R
import com.example.authapp.data.remote.CloudinaryUploader
import com.example.authapp.domain.repository.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

    @Inject
    lateinit var cloudinaryUploader: CloudinaryUploader

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvPhone: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnEdit: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutView: View
    private lateinit var layoutEdit: View
    private lateinit var ivProfileImage: ImageView
    private lateinit var tvChangePhoto: TextView

    private var selectedImageBytes: ByteArray? = null
    private var currentImageUrl = ""

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {

                ivProfileImage.setImageURI(it)

                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                val stream = ByteArrayOutputStream()

                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)

                selectedImageBytes = stream.toByteArray()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvName      = findViewById(R.id.tvName)
        tvEmail     = findViewById(R.id.tvEmail)
        tvRole      = findViewById(R.id.tvRole)
        tvPhone     = findViewById(R.id.tvPhone)
        etName      = findViewById(R.id.etName)
        etPhone     = findViewById(R.id.etPhone)
        btnEdit     = findViewById(R.id.btnEdit)
        btnSave     = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        layoutView  = findViewById(R.id.layoutView)
        layoutEdit  = findViewById(R.id.layoutEdit)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvChangePhoto = findViewById(R.id.tvChangePhoto)

        supportActionBar?.apply {
            title = "My Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        loadProfile()

        btnEdit.setOnClickListener {

            layoutView.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE

            btnEdit.visibility = View.GONE
            btnSave.visibility = View.VISIBLE

            tvChangePhoto.visibility = View.VISIBLE
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        ivProfileImage.setOnClickListener {

            if (layoutEdit.visibility == View.VISIBLE) {
                pickImage.launch("image/*")
            }

        }

        tvChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun loadProfile() {
        val uid = authRepository.getCurrentUid() ?: return
        lifecycleScope.launch {
            val result = authRepository.getUserFromFirestore(uid)
            if (result.isSuccess) {
                val user = result.getOrThrow()

                currentImageUrl = user.profileImageUrl

                ivProfileImage.load(currentImageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_pet_placeholder)
                    error(R.drawable.ic_pet_placeholder)
                }

                tvName.text  = user.displayName.ifEmpty { "Not set" }
                tvEmail.text = user.email.ifEmpty { "Not set" }
                tvPhone.text = user.phoneNumber.ifEmpty { "Not set" }
                tvRole.text  = when (user.role) {
                    "pet_owner"    -> "Pet Owner"
                    "veterinarian" -> "Veterinarian"
                    else           -> "Unknown"
                }
                // Pre-fill edit fields
                etName.setText(user.displayName)
                etPhone.setText(user.phoneNumber)
            }
        }
    }

    private fun saveProfile() {
        val name  = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (name.isBlank()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = authRepository.getCurrentUid() ?: return
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled      = false

        lifecycleScope.launch {

            var imageUrl = currentImageUrl

            if (selectedImageBytes != null) {

                val uploadResult = cloudinaryUploader.uploadImage(
                    imageBytes = selectedImageBytes!!,
                    folder = "pawconnect/users/$uid"
                )

                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrThrow()
                }

            }

            val result = authRepository.updateUserProfile(
                uid = uid,
                displayName = name,
                phone = phone,
                profileImageUrl = imageUrl
            )

            progressBar.visibility = View.GONE
            btnSave.isEnabled      = true

            if (result.isSuccess) {
                Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()

                layoutEdit.visibility = View.GONE
                layoutView.visibility = View.VISIBLE

                btnEdit.visibility = View.VISIBLE
                btnSave.visibility = View.GONE

                tvChangePhoto.visibility = View.GONE

                selectedImageBytes = null

                loadProfile()
            } else {
                Toast.makeText(this@ProfileActivity, "Failed to update", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}