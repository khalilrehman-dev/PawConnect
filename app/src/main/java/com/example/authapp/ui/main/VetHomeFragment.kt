package com.example.authapp.ui.main

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
import com.example.authapp.presentation.home.VetHomeUiState
import com.example.authapp.presentation.home.VetHomeViewModel
import com.example.authapp.ui.Vets.VetProfileSetupActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.Intent


@AndroidEntryPoint
class VetHomeFragment :
    Fragment(R.layout.fragment_vet_home) {

    private val viewModel:
            VetHomeViewModel by viewModels()


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


    private lateinit var cardProfileReminder:
            MaterialCardView

    private lateinit var btnCompleteProfile:
            MaterialButton


    private lateinit var cardPending:
            MaterialCardView

    private lateinit var cardPatients:
            MaterialCardView

    private lateinit var cardMessages:
            MaterialCardView


    private lateinit var tvPendingCount:
            TextView

    private lateinit var tvPatientCount:
            TextView

    private lateinit var tvUnreadCount:
            TextView


    private lateinit var cardNextAppointment:
            MaterialCardView

    private lateinit var layoutAppointmentDetails:
            LinearLayout

    private lateinit var layoutNoAppointment:
            LinearLayout

    private lateinit var tvAppointmentPet:
            TextView

    private lateinit var tvAppointmentDateTime:
            TextView

    private lateinit var tvAppointmentNote:
            TextView

    private lateinit var chipAppointmentStatus:
            Chip


    private lateinit var btnViewAppointments:
            MaterialButton

    private lateinit var btnProfessionalProfile:
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
         * Refresh when returning from appointments,
         * patients or professional profile.
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


        cardProfileReminder =
            view.findViewById(
                R.id.cardProfileReminder
            )

        btnCompleteProfile =
            view.findViewById(
                R.id.btnCompleteProfile
            )


        cardPending =
            view.findViewById(
                R.id.cardPending
            )

        cardPatients =
            view.findViewById(
                R.id.cardPatients
            )

        cardMessages =
            view.findViewById(
                R.id.cardMessages
            )


        tvPendingCount =
            view.findViewById(
                R.id.tvPendingCount
            )

        tvPatientCount =
            view.findViewById(
                R.id.tvPatientCount
            )

        tvUnreadCount =
            view.findViewById(
                R.id.tvUnreadCount
            )


        cardNextAppointment =
            view.findViewById(
                R.id.cardNextAppointment
            )

        layoutAppointmentDetails =
            view.findViewById(
                R.id.layoutAppointmentDetails
            )

        layoutNoAppointment =
            view.findViewById(
                R.id.layoutNoAppointment
            )

        tvAppointmentPet =
            view.findViewById(
                R.id.tvAppointmentPet
            )

        tvAppointmentDateTime =
            view.findViewById(
                R.id.tvAppointmentDateTime
            )

        tvAppointmentNote =
            view.findViewById(
                R.id.tvAppointmentNote
            )

        chipAppointmentStatus =
            view.findViewById(
                R.id.chipAppointmentStatus
            )


        btnViewAppointments =
            view.findViewById(
                R.id.btnViewAppointments
            )

        btnProfessionalProfile =
            view.findViewById(
                R.id.btnProfessionalProfile
            )
    }


    private fun setupClicks() {

        ivProfile.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.vetProfileDestination
                )
        }


        cardPending.setOnClickListener {

            openAppointments()
        }


        cardPatients.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.vetPatientsDestination
                )
        }


        cardMessages.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.vetMessagesDestination
                )
        }


        cardNextAppointment.setOnClickListener {

            openAppointments()
        }


        btnViewAppointments.setOnClickListener {

            openAppointments()
        }


        btnCompleteProfile.setOnClickListener {

            openProfessionalProfile()
        }


        btnProfessionalProfile.setOnClickListener {

            openProfessionalProfile()
        }


        btnRetryHome.setOnClickListener {

            viewModel.loadHome()
        }
    }


    private fun openAppointments() {

        findNavController()
            .navigate(
                R.id.vetAppointmentsDestination
            )
    }


    private fun openProfessionalProfile() {

        startActivity(
            Intent(
                requireContext(),
                VetProfileSetupActivity::class.java
            )
        )
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

                                    VetHomeUiState.Loading -> {

                                        showLoading()
                                    }


                                    is VetHomeUiState.Success -> {

                                        showContent(
                                            state
                                        )
                                    }


                                    is VetHomeUiState.Error -> {

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
        state: VetHomeUiState.Success
    ) {

        progressHome.visibility =
            View.GONE

        layoutHomeError.visibility =
            View.GONE

        homeContent.visibility =
            View.VISIBLE


        // ─────────────────────────────────────────
        // Greeting
        // ─────────────────────────────────────────

        val cleanName =
            state.displayName
                .trim()
                .removePrefix("Dr.")
                .trim()


        val firstName =
            cleanName
                .substringBefore(" ")
                .ifBlank {
                    "Doctor"
                }


        tvGreeting.text =
            "Hello, Dr. $firstName"


        // ─────────────────────────────────────────
        // Profile image
        // ─────────────────────────────────────────

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
        // Professional profile reminder
        // ─────────────────────────────────────────

        cardProfileReminder.visibility =
            if (
                state.isProfileComplete == false
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }


        // ─────────────────────────────────────────
        // Counts
        // ─────────────────────────────────────────

        tvPendingCount.text =
            state.pendingRequestCount
                .toString()

        tvPatientCount.text =
            state.patientCount
                .toString()

        tvUnreadCount.text =
            state.unreadCount
                .toString()


        // ─────────────────────────────────────────
        // Next appointment
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

        tvAppointmentPet.text =
            appointment.petName
                .ifBlank {
                    "Patient"
                }


        tvAppointmentDateTime.text =
            "${appointment.date} • ${appointment.time}"


        if (
            appointment.note.isBlank()
        ) {

            tvAppointmentNote.visibility =
                View.GONE

        } else {

            tvAppointmentNote.text =
                appointment.note

            tvAppointmentNote.visibility =
                View.VISIBLE
        }


        when (
            appointment.status.lowercase()
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