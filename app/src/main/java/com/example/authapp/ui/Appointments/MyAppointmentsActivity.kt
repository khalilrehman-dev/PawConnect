package com.example.authapp.ui.Appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Appointment
import com.example.authapp.presentation.appointments.AppointmentListState
import com.example.authapp.presentation.appointments.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyAppointmentsActivity : AppCompatActivity() {

    private val viewModel: AppointmentViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: OwnerAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        tvEmpty      = findViewById(R.id.tvEmpty)

        adapter = OwnerAppointmentAdapter(
            onCancel = { appointment ->

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cancel appointment?")
                    .setMessage(
                        "Are you sure you want to cancel this appointment?"
                    )
                    .setNegativeButton(
                        "Keep Appointment",
                        null
                    )
                    .setPositiveButton(
                        "Cancel"
                    ) { _, _ ->

                        viewModel.cancelAppointment(
                            appointment.id
                        )
                    }
                    .show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        supportActionBar?.apply {
            title = "My Appointments"
            setDisplayHomeAsUpEnabled(true)
        }

        observeViewModel()
        viewModel.loadOwnerAppointments()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appointmentsState.collect { state ->
                    when (state) {
                        is AppointmentListState.Idle    -> { }
                        is AppointmentListState.Loading -> {
                            progressBar.visibility  = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility      = View.GONE
                        }
                        is AppointmentListState.Success -> {
                            progressBar.visibility  = View.GONE
                            tvEmpty.visibility      = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            adapter.submitList(state.appointments)
                        }
                        is AppointmentListState.Empty -> {
                            progressBar.visibility  = View.GONE
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility      = View.VISIBLE
                        }
                        is AppointmentListState.Error -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MyAppointmentsActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class OwnerAppointmentAdapter(
    private val onCancel: (Appointment) -> Unit
) : RecyclerView.Adapter<OwnerAppointmentAdapter.ViewHolder>() {
    private val items = mutableListOf<Appointment>()

    fun submitList(list: List<Appointment>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_appointment_owner, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvVetName: TextView  = itemView.findViewById(R.id.tvVetName)
        private val tvClinic: TextView   = itemView.findViewById(R.id.tvClinic)
        private val tvPetName: TextView  = itemView.findViewById(R.id.tvPetName)
        private val tvDate: TextView     = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView     = itemView.findViewById(R.id.tvTime)
        private val tvNote: TextView     = itemView.findViewById(R.id.tvNote)
        private val tvStatus: TextView   = itemView.findViewById(R.id.tvStatus)

        private val btnCancelAppointment: Button =
            itemView.findViewById(
                R.id.btnCancelAppointment
            )

        fun bind(a: Appointment) {
            tvVetName.text = "Dr. ${a.vetName}"
            tvClinic.text  = a.clinicName
            tvPetName.text = "Pet: ${a.petName}"
            tvDate.text    = a.date
            tvTime.text    = a.time
            tvNote.text    = if (a.note.isBlank()) "" else "Note: ${a.note}"

            // Status badge
            val (label, color) =
                when (a.status) {

                    "accepted" ->
                        "✅ Accepted" to "#2E7D32"

                    "rejected" ->
                        "❌ Rejected" to "#C62828"

                    "cancelled" ->
                        "Cancelled" to "#757575"

                    else ->
                        "⏳ Pending" to "#F57F17"
                }
            btnCancelAppointment.visibility =
                if (
                    a.status == "pending" ||
                    a.status == "accepted"
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            btnCancelAppointment.setOnClickListener {
                onCancel(a)
            }

            tvStatus.text = label
            tvStatus.setTextColor(android.graphics.Color.parseColor(color))
        }
    }
}