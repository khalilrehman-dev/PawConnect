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
import androidx.navigation.fragment.findNavController
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
class MyAppointmentsFragment :
    Fragment(R.layout.fragment_my_appointments) {

    private val viewModel:
            AppointmentViewModel by viewModels()


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            OwnerAppointmentFragmentAdapter


    private lateinit var progressAppointments:
            CircularProgressIndicator


    private lateinit var layoutEmpty:
            View

    private lateinit var tvEmptyTitle:
            TextView

    private lateinit var tvEmptyMessage:
            TextView

    private lateinit var btnFindVet:
            MaterialButton


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
         * Refresh appointments whenever
         * the user returns to this tab.
         */
        viewModel.loadOwnerAppointments()
    }


    private fun bindViews(
        view: View
    ) {

        recyclerView =
            view.findViewById(
                R.id.recyclerAppointments
            )


        progressAppointments =
            view.findViewById(
                R.id.progressAppointments
            )


        layoutEmpty =
            view.findViewById(
                R.id.layoutAppointmentsEmpty
            )

        tvEmptyTitle =
            view.findViewById(
                R.id.tvAppointmentsEmptyTitle
            )

        tvEmptyMessage =
            view.findViewById(
                R.id.tvAppointmentsEmptyMessage
            )

        btnFindVet =
            view.findViewById(
                R.id.btnFindVetFromAppointments
            )


        layoutError =
            view.findViewById(
                R.id.layoutAppointmentsError
            )

        tvAppointmentsError =
            view.findViewById(
                R.id.tvAppointmentsError
            )

        btnRetry =
            view.findViewById(
                R.id.btnRetryAppointments
            )


        chipAll =
            view.findViewById(
                R.id.chipAllAppointments
            )

        chipPending =
            view.findViewById(
                R.id.chipPendingAppointments
            )

        chipAccepted =
            view.findViewById(
                R.id.chipAcceptedAppointments
            )

        chipClosed =
            view.findViewById(
                R.id.chipClosedAppointments
            )
    }


    private fun setupRecyclerView() {

        adapter =
            OwnerAppointmentFragmentAdapter(
                onCancel = { appointment ->

                    showCancelConfirmation(
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

        btnFindVet.setOnClickListener {

            findNavController()
                .navigate(
                    R.id.ownerDiscoverDestination
                )
        }


        btnRetry.setOnClickListener {

            viewModel.loadOwnerAppointments()
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
                         * One-time appointment events.
                         *
                         * Cancellation failures can reach
                         * the screen through this flow.
                         */
                        launch {

                            viewModel
                                .events
                                .collect { event ->

                                    when (event) {

                                        AppointmentEvent.BookingSuccess -> {

                                            /*
                                             * Booking success is not
                                             * used by this screen.
                                             */
                                        }


                                        is AppointmentEvent.Error -> {

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


    private fun showCancelConfirmation(
        appointment: Appointment
    ) {

        val cleanVetName =
            appointment.vetName
                .trim()
                .removePrefix("Dr.")
                .trim()


        val message =
            if (
                cleanVetName.isBlank()
            ) {

                "Are you sure you want to cancel this appointment?"

            } else {

                "Cancel your appointment with Dr. $cleanVetName?"
            }


        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                "Cancel appointment?"
            )
            .setMessage(
                message
            )
            .setNegativeButton(
                "Keep appointment",
                null
            )
            .setPositiveButton(
                "Cancel appointment"
            ) { _, _ ->

                viewModel.cancelAppointment(
                    appointment.id
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

                        "You do not have any pending appointment requests."
                    }


                    FILTER_ACCEPTED -> {

                        "You do not have any accepted appointments."
                    }


                    FILTER_CLOSED -> {

                        "You do not have any cancelled or rejected appointments."
                    }


                    else -> {

                        "No appointments match this filter."
                    }
                }


            /*
             * If appointments exist but the selected
             * filter has no results, Find Vet button
             * is unnecessary.
             */
            btnFindVet.visibility =
                View.GONE

        } else {

            tvEmptyTitle.text =
                "No appointments yet"


            tvEmptyMessage.text =
                "Find a veterinarian and request an appointment for one of your pets."


            btnFindVet.visibility =
                View.VISIBLE
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
 * Simple RecyclerView adapter for the owner
 * appointments tab.
 *
 * We keep it in this file during the current
 * migration so the screen remains easy to
 * understand and maintain.
 */
class OwnerAppointmentFragmentAdapter(
    private val onCancel: (Appointment) -> Unit
) : RecyclerView.Adapter<
        OwnerAppointmentFragmentAdapter.ViewHolder
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
                    R.layout.item_appointment_owner,
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

        private val tvVetName:
                TextView =
            itemView.findViewById(
                R.id.tvVetName
            )


        private val tvClinic:
                TextView =
            itemView.findViewById(
                R.id.tvClinic
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
                R.id.cardAppointmentNote
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


        private val btnCancel:
                MaterialButton =
            itemView.findViewById(
                R.id.btnCancelAppointment
            )


        fun bind(
            appointment: Appointment
        ) {

            bindVet(
                appointment
            )

            bindPet(
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

            bindCancelAction(
                appointment
            )
        }


        private fun bindVet(
            appointment: Appointment
        ) {

            val cleanVetName =
                appointment.vetName
                    .trim()
                    .removePrefix("Dr.")
                    .trim()


            tvVetName.text =
                if (
                    cleanVetName.isBlank()
                ) {

                    "Veterinarian"

                } else {

                    "Dr. $cleanVetName"
                }


            tvClinic.text =
                appointment.clinicName
                    .ifBlank {

                        "Clinic information unavailable"
                    }
        }


        private fun bindPet(
            appointment: Appointment
        ) {

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

                /*
                 * Hide the entire note card,
                 * not just the TextView.
                 */
                cardNote.visibility =
                    View.GONE

            } else {

                tvNote.text =
                    appointment.note

                cardNote.visibility =
                    View.VISIBLE
            }
        }


        private fun bindCancelAction(
            appointment: Appointment
        ) {

            val canCancel =
                appointment.status
                    .equals(
                        "pending",
                        ignoreCase = true
                    ) ||

                        appointment.status
                            .equals(
                                "accepted",
                                ignoreCase = true
                            )


            btnCancel.visibility =
                if (canCancel) {

                    View.VISIBLE

                } else {

                    View.GONE
                }


            /*
             * RecyclerView reuses views.
             * Always reset the listener during bind.
             */
            btnCancel.setOnClickListener(
                null
            )


            if (canCancel) {

                btnCancel.setOnClickListener {

                    onCancel(
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