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
class VetAppointmentsActivity : AppCompatActivity() {

    private val viewModel: AppointmentViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: VetAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_appointments)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        tvEmpty      = findViewById(R.id.tvEmpty)

        adapter = VetAppointmentAdapter(
            onAccept = { appointment -> viewModel.updateStatus(appointment.id, "accepted") },
            onReject = { appointment -> viewModel.updateStatus(appointment.id, "rejected") }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        supportActionBar?.apply {
            title = "Appointments"
            setDisplayHomeAsUpEnabled(true)
        }

        observeViewModel()
        viewModel.loadVetAppointments()
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
                            Toast.makeText(this@VetAppointmentsActivity, state.message, Toast.LENGTH_LONG).show()
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

class VetAppointmentAdapter(
    private val onAccept: (Appointment) -> Unit,
    private val onReject: (Appointment) -> Unit
) : RecyclerView.Adapter<VetAppointmentAdapter.ViewHolder>() {

    private val items = mutableListOf<Appointment>()

    fun submitList(list: List<Appointment>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_appointment_vet, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOwnerName: TextView  = itemView.findViewById(R.id.tvOwnerName)
        private val tvPetName: TextView    = itemView.findViewById(R.id.tvPetName)
        private val tvDate: TextView       = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView       = itemView.findViewById(R.id.tvTime)
        private val tvNote: TextView       = itemView.findViewById(R.id.tvNote)
        private val tvStatus: TextView     = itemView.findViewById(R.id.tvStatus)
        private val btnAccept: Button      = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button      = itemView.findViewById(R.id.btnReject)
        private val layoutActions: View    = itemView.findViewById(R.id.layoutActions)

        fun bind(a: Appointment) {
            tvOwnerName.text = "Owner ID: ${a.petOwnerId.take(8)}..."
            tvPetName.text   = "Pet: ${a.petName}"
            tvDate.text      = a.date
            tvTime.text      = a.time
            tvNote.text      = if (a.note.isBlank()) "" else "Note: ${a.note}"

            val (label, color) = when (a.status) {
                "accepted" -> "✅ Accepted" to "#2E7D32"
                "rejected" -> "❌ Rejected" to "#C62828"
                else       -> "⏳ Pending"  to "#F57F17"
            }
            tvStatus.text = label
            tvStatus.setTextColor(android.graphics.Color.parseColor(color))

            // Show accept/reject only for pending
            layoutActions.visibility = if (a.status == "pending") View.VISIBLE else View.GONE

            btnAccept.setOnClickListener { onAccept(a) }
            btnReject.setOnClickListener { onReject(a) }
        }
    }
}