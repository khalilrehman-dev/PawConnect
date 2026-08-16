package com.example.authapp.ui.Vets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.example.authapp.R
import com.example.authapp.ui.Appointments.BookAppointmentActivity
import com.example.authapp.ui.Chat.ChatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class VetDetailActivity :
    AppCompatActivity() {

    private lateinit var toolbar:
            MaterialToolbar

    private lateinit var ivVetPhoto:
            ShapeableImageView

    private lateinit var tvVetName:
            TextView

    private lateinit var tvSpecialization:
            TextView

    private lateinit var tvClinicName:
            TextView

    private lateinit var tvCity:
            TextView

    private lateinit var tvAddress:
            TextView

    private lateinit var tvPhone:
            TextView

    private lateinit var tvExperience:
            TextView

    private lateinit var btnBookAppointment:
            MaterialButton

    private lateinit var btnMessage:
            MaterialButton


    private var vetUid =
        ""

    private var vetName =
        ""

    private var clinicName =
        ""


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        enableEdgeToEdge()

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_vet_detail
        )


        bindViews()

        applySystemInsets()

        setupToolbar()

        bindVetData()

        setupClicks()
    }


    private fun bindViews() {

        toolbar =
            findViewById(
                R.id.toolbarVetDetail
            )

        ivVetPhoto =
            findViewById(
                R.id.ivVetPhoto
            )

        tvVetName =
            findViewById(
                R.id.tvVetName
            )

        tvSpecialization =
            findViewById(
                R.id.tvSpecialization
            )

        tvClinicName =
            findViewById(
                R.id.tvClinicName
            )

        tvCity =
            findViewById(
                R.id.tvCity
            )

        tvAddress =
            findViewById(
                R.id.tvAddress
            )

        tvPhone =
            findViewById(
                R.id.tvPhone
            )

        tvExperience =
            findViewById(
                R.id.tvExperience
            )

        btnBookAppointment =
            findViewById(
                R.id.btnBookAppointment
            )

        btnMessage =
            findViewById(
                R.id.btnMessage
            )
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootVetDetail
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
                "Veterinarian Profile"

            setDisplayHomeAsUpEnabled(
                true
            )
        }
    }


    private fun bindVetData() {

        vetUid =
            intent.getStringExtra(
                "vetUid"
            ).orEmpty()


        vetName =
            intent.getStringExtra(
                "vetName"
            ).orEmpty()


        clinicName =
            intent.getStringExtra(
                "clinic"
            ).orEmpty()


        val city =
            intent.getStringExtra(
                "city"
            ).orEmpty()


        val address =
            intent.getStringExtra(
                "address"
            ).orEmpty()


        val phone =
            intent.getStringExtra(
                "phone"
            ).orEmpty()


        val specialization =
            intent.getStringExtra(
                "spec"
            ).orEmpty()


        val years =
            intent.getIntExtra(
                "years",
                0
            )


        val imageUrl =
            intent.getStringExtra(
                "imageUrl"
            ).orEmpty()


        val cleanName =
            vetName
                .trim()
                .removePrefix("Dr.")
                .trim()


        tvVetName.text =
            if (cleanName.isBlank()) {

                "Veterinarian"

            } else {

                "Dr. $cleanName"
            }


        tvSpecialization.text =
            specialization
                .ifBlank {
                    "General Practice"
                }


        tvClinicName.text =
            clinicName
                .ifBlank {
                    "Clinic information not provided"
                }


        tvCity.text =
            city
                .ifBlank {
                    "City not provided"
                }


        tvAddress.text =
            address
                .ifBlank {
                    "Address not provided"
                }


        tvPhone.text =
            phone
                .ifBlank {
                    "Phone number not provided"
                }


        tvExperience.text =
            when (years) {

                1 ->
                    "1 year of experience"

                else ->
                    "$years years of experience"
            }


        if (imageUrl.isBlank()) {

            ivVetPhoto.setImageResource(
                R.drawable.ic_profile
            )

        } else {

            ivVetPhoto.load(
                imageUrl
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


        /*
         * A missing UID means this profile cannot
         * safely start booking or messaging.
         */
        val actionsEnabled =
            vetUid.isNotBlank()


        btnBookAppointment.isEnabled =
            actionsEnabled

        btnMessage.isEnabled =
            actionsEnabled
    }


    private fun setupClicks() {

        btnBookAppointment
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        BookAppointmentActivity::class.java
                    ).apply {

                        putExtra(
                            "vetId",
                            vetUid
                        )

                        putExtra(
                            "vetName",
                            vetName
                        )

                        putExtra(
                            "clinic",
                            clinicName
                        )
                    }
                )
            }


        btnMessage
            .setOnClickListener {

                val cleanName =
                    vetName
                        .trim()
                        .removePrefix("Dr.")
                        .trim()


                val displayName =
                    if (cleanName.isBlank()) {

                        "Veterinarian"

                    } else {

                        "Dr. $cleanName"
                    }


                startActivity(
                    Intent(
                        this,
                        ChatActivity::class.java
                    ).apply {

                        putExtra(
                            "otherUserId",
                            vetUid
                        )

                        putExtra(
                            "otherName",
                            displayName
                        )
                    }
                )
            }
    }


    override fun onSupportNavigateUp():
            Boolean {

        onBackPressedDispatcher
            .onBackPressed()

        return true
    }
}