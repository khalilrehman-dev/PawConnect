package com.example.authapp.ui.Appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Appointment
import com.example.authapp.presentation.appointments.AppointmentEvent
import com.example.authapp.presentation.appointments.AppointmentListState
import com.example.authapp.presentation.appointments.AppointmentViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class VetAppointmentsFragment :
    Fragment(R.layout.fragment_vet_appointments) {

    private val viewModel:
            AppointmentViewModel by viewModels()


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            VetAppointmentFragmentAdapter


    private lateinit var progressAppointments:
            CircularProgressIndicator


    private lateinit var layoutEmpty:
            View

    private lateinit var tvEmptyTitle:
            TextView

    private lateinit var tvEmptyMessage:
            TextView


    private lateinit var layoutError:
            View

    private lateinit var tvAppointmentsError:
            TextView

    private lateinit var btnRetry:
            MaterialButton


    private lateinit var chipAll:
            Chip

    private lateinit var chipPending:
            Chip

    private lateinit var chipAccepted:
            Chip

    private lateinit var chipClosed:
            Chip


    private var allAppointments:
            List<Appointment> =
        emptyList()


    private var selectedFilter =
        FILTER_ALL


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        bindViews(view)

        setupRecyclerView()

        setupFilters()

        setupClicks()

        observeViewModel()
    }


    override fun onResume() {
        super.onResume()

        /*
         * Refresh whenever the veterinarian
         * returns to the Appointments tab.
         */
        viewModel.loadVetAppointments()
    }


    private fun bindViews(
        view: View
    ) {

        recyclerView =
            view.findViewById(
                R.id.recyclerVetAppointments
            )


        progressAppointments =
            view.findViewById(
                R.id.progressVetAppointments
            )


        layoutEmpty =
            view.findViewById(
                R.id.layoutVetAppointmentsEmpty
            )

        tvEmptyTitle =
            view.findViewById(
                R.id.tvVetAppointmentsEmptyTitle
            )

        tvEmptyMessage =
            view.findViewById(
                R.id.tvVetAppointmentsEmptyMessage
            )


        layoutError =
            view.findViewById(
                R.id.layoutVetAppointmentsError
            )

        tvAppointmentsError =
            view.findViewById(
                R.id.tvVetAppointmentsError
            )

        btnRetry =
            view.findViewById(
                R.id.btnRetryVetAppointments
            )


        chipAll =
            view.findViewById(
                R.id.chipAllVetAppointments
            )

        chipPending =
            view.findViewById(
                R.id.chipPendingVetAppointments
            )

        chipAccepted =
            view.findViewById(
                R.id.chipAcceptedVetAppointments
            )

        chipClosed =
            view.findViewById(
                R.id.chipClosedVetAppointments
            )
    }


    private fun setupRecyclerView() {

        adapter =
            VetAppointmentFragmentAdapter(

                onAccept = { appointment ->

                    showAcceptConfirmation(
                        appointment
                    )
                },

                onReject = { appointment ->

                    showRejectConfirmation(
                        appointment
                    )
                }
            )


        recyclerView.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        recyclerView.adapter =
            adapter
    }


    private fun setupFilters() {

        chipAll.setOnClickListener {

            selectedFilter =
                FILTER_ALL

            applyCurrentFilter()
        }


        chipPending.setOnClickListener {

            selectedFilter =
                FILTER_PENDING

            applyCurrentFilter()
        }


        chipAccepted.setOnClickListener {

            selectedFilter =
                FILTER_ACCEPTED

            applyCurrentFilter()
        }


        chipClosed.setOnClickListener {

            selectedFilter =
                FILTER_CLOSED

            applyCurrentFilter()
        }
    }


    private fun setupClicks() {

        btnRetry.setOnClickListener {

            viewModel.loadVetAppointments()
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

                        /*
                         * Appointment list state
                         */
                        launch {

                            viewModel
                                .appointmentsState
                                .collect { state ->

                                    when (state) {

                                        AppointmentListState.Idle -> {

                                            /*
                                             * Initial loader is already
                                             * visible in XML.
                                             */
                                        }


                                        AppointmentListState.Loading -> {

                                            showLoading()
                                        }


                                        is AppointmentListState.Success -> {

                                            allAppointments =
                                                state.appointments

                                            applyCurrentFilter()
                                        }


                                        AppointmentListState.Empty -> {

                                            allAppointments =
                                                emptyList()

                                            showEmpty(
                                                filtered = false
                                            )
                                        }


                                        is AppointmentListState.Error -> {

                                            showError(
                                                state.message
                                            )
                                        }
                                    }
                                }
                        }


                        /*
                         * Repository / status-update errors.
                         */
                        launch {

                            viewModel
                                .events
                                .collect { event ->

                                    when (event) {

                                        is AppointmentEvent.Error -> {

                                            Toast.makeText(
                                                requireContext(),
                                                event.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }


                                        AppointmentEvent.BookingSuccess -> {

                                            /*
                                             * This event belongs to
                                             * the owner booking flow.
                                             */
                                        }
                                    }
                                }
                        }
                    }
            }
    }


    private fun applyCurrentFilter() {

        val filteredAppointments =
            when (selectedFilter) {

                FILTER_PENDING -> {

                    allAppointments.filter { appointment ->

                        appointment.status
                            .equals(
                                "pending",
                                ignoreCase = true
                            )
                    }
                }


                FILTER_ACCEPTED -> {

                    allAppointments.filter { appointment ->

                        appointment.status
                            .equals(
                                "accepted",
                                ignoreCase = true
                            )
                    }
                }


                FILTER_CLOSED -> {

                    allAppointments.filter { appointment ->

                        appointment.status
                            .equals(
                                "rejected",
                                ignoreCase = true
                            ) ||

                                appointment.status
                                    .equals(
                                        "cancelled",
                                        ignoreCase = true
                                    )
                    }
                }


                else -> {

                    allAppointments
                }
            }


        if (
            filteredAppointments.isEmpty()
        ) {

            showEmpty(
                filtered =
                    allAppointments.isNotEmpty()
            )

        } else {

            showAppointments(
                filteredAppointments
            )
        }
    }


    private fun showAcceptConfirmation(
        appointment: Appointment
    ) {

        val petName =
            appointment.petName
                .ifBlank {
                    "this pet"
                }


        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                "Accept appointment?"
            )
            .setMessage(
                "Accept the appointment request for $petName on ${appointment.date} at ${appointment.time}?"
            )
            .setNegativeButton(
                "Not now",
                null
            )
            .setPositiveButton(
                "Accept"
            ) { _, _ ->

                viewModel.updateStatus(
                    appointment.id,
                    "accepted"
                )
            }
            .show()
    }


    private fun showRejectConfirmation(
        appointment: Appointment
    ) {

        val petName =
            appointment.petName
                .ifBlank {
                    "this pet"
                }


        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                "Reject appointment?"
            )
            .setMessage(
                "Reject the appointment request for $petName? The reserved time slot will be released."
            )
            .setNegativeButton(
                "Keep request",
                null
            )
            .setPositiveButton(
                "Reject"
            ) { _, _ ->

                viewModel.updateStatus(
                    appointment.id,
                    "rejected"
                )
            }
            .show()
    }


    private fun showLoading() {

        progressAppointments.visibility =
            View.VISIBLE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE
    }


    private fun showAppointments(
        appointments: List<Appointment>
    ) {

        progressAppointments.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        adapter.submitList(
            appointments
        )


        recyclerView.visibility =
            View.VISIBLE
    }


    private fun showEmpty(
        filtered: Boolean
    ) {

        progressAppointments.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        if (filtered) {

            tvEmptyTitle.text =
                "No matching appointments"


            tvEmptyMessage.text =
                when (selectedFilter) {

                    FILTER_PENDING -> {

                        "There are no pending appointment requests."
                    }


                    FILTER_ACCEPTED -> {

                        "There are no accepted appointments."
                    }


                    FILTER_CLOSED -> {

                        "There are no rejected or cancelled appointments."
                    }


                    else -> {

                        "No appointments match this filter."
                    }
                }

        } else {

            tvEmptyTitle.text =
                "No appointments yet"


            tvEmptyMessage.text =
                "New appointment requests from pet owners will appear here."
        }


        layoutEmpty.visibility =
            View.VISIBLE
    }


    private fun showError(
        message: String
    ) {

        progressAppointments.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE


        tvAppointmentsError.text =
            message


        layoutError.visibility =
            View.VISIBLE
    }


    companion object {

        private const val FILTER_ALL =
            "all"

        private const val FILTER_PENDING =
            "pending"

        private const val FILTER_ACCEPTED =
            "accepted"

        private const val FILTER_CLOSED =
            "closed"
    }
}


/*
 * Simple adapter kept in the same file during
 * migration so the screen stays easy to follow.
 */
class VetAppointmentFragmentAdapter(
    private val onAccept: (Appointment) -> Unit,
    private val onReject: (Appointment) -> Unit
) : RecyclerView.Adapter<
        VetAppointmentFragmentAdapter.ViewHolder
        >() {

    private val items =
        mutableListOf<Appointment>()


    fun submitList(
        appointments: List<Appointment>
    ) {

        items.clear()

        items.addAll(
            appointments
        )

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_appointment_vet,
                    parent,
                    false
                )


        return ViewHolder(
            view
        )
    }


    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }


    override fun getItemCount():
            Int {

        return items.size
    }


    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(
        itemView
    ) {

        /*
         * Keep tvOwnerName ID because the legacy
         * VetAppointmentsActivity still references it.
         */
        private val tvOwnerName:
                TextView =
            itemView.findViewById(
                R.id.tvOwnerName
            )


        private val tvPetName:
                TextView =
            itemView.findViewById(
                R.id.tvPetName
            )


        private val tvDate:
                TextView =
            itemView.findViewById(
                R.id.tvDate
            )


        private val tvTime:
                TextView =
            itemView.findViewById(
                R.id.tvTime
            )


        private val cardNote:
                View =
            itemView.findViewById(
                R.id.cardVetAppointmentNote
            )


        private val tvNote:
                TextView =
            itemView.findViewById(
                R.id.tvNote
            )


        private val chipStatus:
                Chip =
            itemView.findViewById(
                R.id.tvStatus
            )


        private val layoutActions:
                View =
            itemView.findViewById(
                R.id.layoutActions
            )


        private val btnAccept:
                MaterialButton =
            itemView.findViewById(
                R.id.btnAccept
            )


        private val btnReject:
                MaterialButton =
            itemView.findViewById(
                R.id.btnReject
            )


        fun bind(
            appointment: Appointment
        ) {

            bindPatient(
                appointment
            )

            bindSchedule(
                appointment
            )

            bindNote(
                appointment
            )

            bindStatus(
                appointment.status
            )

            bindActions(
                appointment
            )
        }


        private fun bindPatient(
            appointment: Appointment
        ) {

            /*
             * Appointment currently stores owner UID,
             * not the owner's display name.
             *
             * Don't expose a raw Firebase UID in the
             * customer-facing UI.
             */
            tvOwnerName.text =
                "Appointment request"


            tvPetName.text =
                appointment.petName
                    .ifBlank {
                        "Pet"
                    }
        }


        private fun bindSchedule(
            appointment: Appointment
        ) {

            tvDate.text =
                appointment.date
                    .ifBlank {
                        "Date unavailable"
                    }


            tvTime.text =
                appointment.time
                    .ifBlank {
                        "Time unavailable"
                    }
        }


        private fun bindNote(
            appointment: Appointment
        ) {

            if (
                appointment.note.isBlank()
            ) {

                cardNote.visibility =
                    View.GONE

            } else {

                tvNote.text =
                    appointment.note

                cardNote.visibility =
                    View.VISIBLE
            }
        }


        private fun bindActions(
            appointment: Appointment
        ) {

            val isPending =
                appointment.status
                    .equals(
                        "pending",
                        ignoreCase = true
                    )


            layoutActions.visibility =
                if (isPending) {

                    View.VISIBLE

                } else {

                    View.GONE
                }


            /*
             * RecyclerView reuses views.
             * Always clear old listeners.
             */
            btnAccept.setOnClickListener(
                null
            )

            btnReject.setOnClickListener(
                null
            )


            if (isPending) {

                btnAccept.setOnClickListener {

                    onAccept(
                        appointment
                    )
                }


                btnReject.setOnClickListener {

                    onReject(
                        appointment
                    )
                }
            }
        }


        private fun bindStatus(
            status: String
        ) {

            when (
                status.lowercase()
            ) {

                "accepted" -> {

                    chipStatus.text =
                        "Accepted"


                    chipStatus
                        .setChipBackgroundColorResource(
                            R.color.paw_success_container
                        )


                    chipStatus.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.paw_success
                        )
                    )
                }


                "rejected" -> {

                    chipStatus.text =
                        "Rejected"


                    chipStatus
                        .setChipBackgroundColorResource(
                            R.color.paw_error_container
                        )


                    chipStatus.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.paw_error
                        )
                    )
                }


                "cancelled" -> {

                    chipStatus.text =
                        "Cancelled"


                    chipStatus
                        .setChipBackgroundColorResource(
                            R.color.paw_surface_variant
                        )


                    chipStatus.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.paw_text_secondary
                        )
                    )
                }


                else -> {

                    chipStatus.text =
                        "Pending"


                    chipStatus
                        .setChipBackgroundColorResource(
                            R.color.paw_warning_container
                        )


                    chipStatus.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.paw_warning
                        )
                    )
                }
            }
        }
    }
}