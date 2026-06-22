package com.example.authapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.authapp.R
import com.example.authapp.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val session = SessionManager(this)
        val auth = FirebaseAuth.getInstance()

        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserContact = findViewById<TextView>(R.id.tvUserContact)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Show user info
        val name = session.getUserName()
        val email = session.getUserEmail()
        val phone = session.getUserPhone()


        tvUserName.text = if (name.isNotEmpty()) name else "User"
        tvUserContact.text = when {
            email.isNotEmpty() -> email
            phone.isNotEmpty() -> phone
            else -> auth.currentUser?.email ?: "No contact info"
        }

        btnLogout.setOnClickListener {
            // Sign out from Firebase
            auth.signOut()
            // Clear local session
            session.logout()
            // Go back to Welcome screen
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}