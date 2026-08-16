package com.example.authapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.authapp.R
import com.example.authapp.model.Appointment
import com.example.authapp.presentation.home.OwnerHomeUiState
import com.example.authapp.presentation.home.OwnerHomeViewModel
import com.example.authapp.ui.ProfileActivity
import com.example.authapp.ui.Vets.FindVetsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class OwnerHomeFragment :
    Fragment(R.layout.fragment_owner_home) {

    private val viewModel:
            OwnerHomeViewModel by viewModels()


    private lateinit var homeContent: View
    private lateinit var progressHome:
            CircularProgressIndicator

    private lateinit var layoutHomeError:
            LinearLayout

    private lateinit var tvHomeError:
            TextView

    private lateinit var btnRetryHome:
            MaterialButton


    private lateinit var tvGreeting:
            TextView

    private lateinit var ivProfile:
            ShapeableImageView


    private lateinit var cardPets:
            MaterialCardView

    private lateinit var cardMessages:
            MaterialCardView

    private lateinit var tvPetCount:
            TextView

    private lateinit var tvUnreadCount:
            TextView


    private lateinit var cardAppointment:
            MaterialCardView

    private lateinit var layoutAppointmentDetails:
            LinearLayout

    private lateinit var layoutNoAppointment:
            LinearLayout

    private lateinit var tvAppointmentVet:
            TextView

    private lateinit var tvAppointmentClinic:
            TextView

    private lateinit var tvAppointmentPet:
            TextView

    private lateinit var tvAppointmentDateTime:
            TextView

    private lateinit var chipAppointmentStatus:
            Chip


    private lateinit var btnFindVet:
            MaterialButton

    private lateinit var btnViewPets:
            MaterialButton


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        bindViews(view)

        setupClicks()

        observeViewModel()
    }


    override fun onResume() {
        super.onResume()

        /*
         * Refresh Home whenever the user returns
         * from Pets, Profile, Appointments, etc.
         */
        viewModel.loadHome()
    }


    private fun bindViews(
        view: View
    ) {

        homeContent =
            view.findViewById(
                R.id.homeContent
            )

        progressHome =
            view.findViewById(
                R.id.progressHome
            )

        layoutHomeError =
            view.findViewById(
                R.id.layoutHomeError
            )

        tvHomeError =
            view.findViewById(
                R.id.tvHomeError
            )

        btnRetryHome =
            view.findViewById(
                R.id.btnRetryHome
            )


        tvGreeting =
            view.findViewById(
                R.id.tvGreeting
            )

        ivProfile =
            view.findViewById(
                R.id.ivProfile
            )


        cardPets =
            view.findViewById(
                R.id.cardPets
            )

        cardMessages =
            view.findViewById(
                R.id.cardMessages
            )

        tvPetCount =
            view.findViewById(
                R.id.tvPetCount
            )

        tvUnreadCount =
            view.findViewById(
                R.id.tvUnreadCount
            )


        cardAppointment =
            view.findViewById(
                R.id.cardAppointment
            )

        layoutAppointmentDetails =
            view.findViewById(
                R.id.layoutAppointmentDetails
            )

        layoutNoAppointment =
            view.findViewById(
                R.id.layoutNoAppointment
            )

        tvAppointmentVet =
            view.findViewById(
                R.id.tvAppointmentVet
            )

        tvAppointmentClinic =
            view.findViewById(
                R.id.tvAppointmentClinic
            )

        tvAppointmentPet =
            view.findViewById(
                R.id.tvAppointmentPet
            )

        tvAppointmentDateTime =
            view.findViewById(
                R.id.tvAppointmentDateTime
            )

        chipAppointmentStatus =
            view.findViewById(
                R.id.chipAppointmentStatus
            )


        btnFindVet =
            view.findViewById(
                R.id.btnFindVet
            )

        btnViewPets =
            view.findViewById(
                R.id.btnViewPets
            )
    }


    private fun setupClicks() {

        ivProfile.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    ProfileActivity::class.java
                )
            )
        }


        cardPets.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.ownerPetsDestination
                )
        }


        cardMessages.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.ownerMessagesDestination
                )
        }


        cardAppointment.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.ownerAppointmentsDestination
                )
        }


        btnViewPets.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.ownerPetsDestination
                )
        }


        btnFindVet.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    FindVetsActivity::class.java
                )
            )
        }


        btnRetryHome.setOnClickListener {

            viewModel.loadHome()
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

                        viewModel.uiState
                            .collect { state ->

                                when (state) {

                                    OwnerHomeUiState.Loading -> {
                                        showLoading()
                                    }


                                    is OwnerHomeUiState.Success -> {

                                        showContent(
                                            state
                                        )
                                    }


                                    is OwnerHomeUiState.Error -> {

                                        showError(
                                            state.message
                                        )
                                    }
                                }
                            }
                    }
            }
    }


    private fun showLoading() {

        progressHome.visibility =
            View.VISIBLE

        homeContent.visibility =
            View.GONE

        layoutHomeError.visibility =
            View.GONE
    }


    private fun showContent(
        state: OwnerHomeUiState.Success
    ) {

        progressHome.visibility =
            View.GONE

        layoutHomeError.visibility =
            View.GONE

        homeContent.visibility =
            View.VISIBLE


        // ─────────────────────────────────────────
        // Header
        // ─────────────────────────────────────────

        val firstName =
            state.displayName
                .trim()
                .substringBefore(" ")
                .ifBlank {
                    "there"
                }


        tvGreeting.text =
            "Hello, $firstName"


        if (
            state.profileImageUrl.isBlank()
        ) {

            ivProfile.setImageResource(
                R.drawable.ic_profile
            )

        } else {

            ivProfile.load(
                state.profileImageUrl
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


        // ─────────────────────────────────────────
        // Overview
        // ─────────────────────────────────────────

        tvPetCount.text =
            state.petCount.toString()

        tvUnreadCount.text =
            state.unreadCount.toString()


        // ─────────────────────────────────────────
        // Appointment
        // ─────────────────────────────────────────

        val appointment =
            state.upcomingAppointment


        if (appointment == null) {

            layoutAppointmentDetails.visibility =
                View.GONE

            layoutNoAppointment.visibility =
                View.VISIBLE

        } else {

            layoutNoAppointment.visibility =
                View.GONE

            layoutAppointmentDetails.visibility =
                View.VISIBLE

            bindAppointment(
                appointment
            )
        }
    }


    private fun bindAppointment(
        appointment: Appointment
    ) {

        val vetName =
            appointment.vetName
                .trim()
                .removePrefix("Dr.")
                .trim()


        tvAppointmentVet.text =
            if (vetName.isBlank()) {
                "Veterinarian"
            } else {
                "Dr. $vetName"
            }


        tvAppointmentClinic.text =
            appointment.clinicName
                .ifBlank {
                    "Clinic details unavailable"
                }


        tvAppointmentPet.text =
            "For ${appointment.petName}"


        tvAppointmentDateTime.text =
            "${appointment.date} • ${appointment.time}"


        when (
            appointment.status
                .lowercase()
        ) {

            "accepted" -> {

                chipAppointmentStatus.text =
                    "Accepted"

                chipAppointmentStatus
                    .setChipBackgroundColorResource(
                        R.color.paw_success_container
                    )

                chipAppointmentStatus
                    .setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.paw_success
                        )
                    )
            }


            else -> {

                chipAppointmentStatus.text =
                    "Pending"

                chipAppointmentStatus
                    .setChipBackgroundColorResource(
                        R.color.paw_warning_container
                    )

                chipAppointmentStatus
                    .setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.paw_warning
                        )
                    )
            }
        }
    }


    private fun showError(
        message: String
    ) {

        progressHome.visibility =
            View.GONE

        homeContent.visibility =
            View.GONE

        tvHomeError.text =
            message

        layoutHomeError.visibility =
            View.VISIBLE
    }
}