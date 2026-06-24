package com.example.authapp.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository

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

        supportActionBar?.apply {
            title = "My Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        loadProfile()

        btnEdit.setOnClickListener {
            layoutView.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE
            btnEdit.visibility    = View.GONE
            btnSave.visibility    = View.VISIBLE
        }

        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfile() {
        val uid = authRepository.getCurrentUid() ?: return
        lifecycleScope.launch {
            val result = authRepository.getUserFromFirestore(uid)
            if (result.isSuccess) {
                val user = result.getOrThrow()
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
            val result = authRepository.updateUserProfile(uid, name, phone)
            progressBar.visibility = View.GONE
            btnSave.isEnabled      = true

            if (result.isSuccess) {
                Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                layoutEdit.visibility = View.GONE
                layoutView.visibility = View.VISIBLE
                loadProfile() // refresh displayed values
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