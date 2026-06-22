package com.example.authapp.ui.Appointments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.authapp.R
import com.example.authapp.model.Pet
import com.example.authapp.presentation.appointments.AppointmentEvent
import com.example.authapp.presentation.appointments.AppointmentViewModel
import com.example.authapp.presentation.appointments.BookingState
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class BookAppointmentActivity : AppCompatActivity() {

    private val viewModel: AppointmentViewModel by viewModels()

    private lateinit var tvVetName: TextView
    private lateinit var tvClinicName: TextView
    private lateinit var spinnerPet: Spinner
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var etNote: TextInputEditText
    private lateinit var btnBook: Button
    private lateinit var progressBar: ProgressBar

    private var vetId: String    = ""
    private var vetName: String  = ""
    private var clinicName: String = ""
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var petList: List<Pet> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        vetId      = intent.getStringExtra("vetId")    ?: ""
        vetName    = intent.getStringExtra("vetName")  ?: ""
        clinicName = intent.getStringExtra("clinic")   ?: ""

        bindViews()
        observeViewModel()
        viewModel.loadMyPets()

        tvVetName.text   = "Dr. $vetName"
        tvClinicName.text = clinicName

        tvDate.setOnClickListener { showDatePicker() }
        tvTime.setOnClickListener { showTimePicker() }

        btnBook.setOnClickListener {
            val selectedPet = petList.getOrNull(spinnerPet.selectedItemPosition)
            if (selectedPet == null) {
                Toast.makeText(this, "Please add a pet first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.bookAppointment(
                vetId      = vetId,
                vetName    = vetName,
                clinicName = clinicName,
                selectedPet = selectedPet,
                date       = selectedDate,
                time       = selectedTime,
                note       = etNote.text.toString()
            )
        }

        supportActionBar?.apply {
            title = "Book Appointment"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun bindViews() {
        tvVetName    = findViewById(R.id.tvVetName)
        tvClinicName = findViewById(R.id.tvClinicName)
        spinnerPet   = findViewById(R.id.spinnerPet)
        tvDate       = findViewById(R.id.tvDate)
        tvTime       = findViewById(R.id.tvTime)
        etNote       = findViewById(R.id.etNote)
        btnBook      = findViewById(R.id.btnBook)
        progressBar  = findViewById(R.id.progressBar)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Load pets into spinner
                launch {
                    viewModel.petsState.collect { pets ->
                        petList = pets
                        val names = if (pets.isEmpty()) listOf("No pets added yet")
                        else pets.map { it.name }
                        spinnerPet.adapter = ArrayAdapter(
                            this@BookAppointmentActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            names
                        )
                    }
                }

                // Booking state
                launch {
                    viewModel.bookingState.collect { state ->
                        when (state) {
                            is BookingState.Idle    -> showLoading(false)
                            is BookingState.Loading -> showLoading(true)
                            is BookingState.Success -> showLoading(false)
                            is BookingState.Error   -> {
                                showLoading(false)
                                Toast.makeText(this@BookAppointmentActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                // Events
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AppointmentEvent.BookingSuccess -> {
                                Toast.makeText(this@BookAppointmentActivity, "Appointment booked!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            is AppointmentEvent.Error -> {
                                Toast.makeText(this@BookAppointmentActivity, event.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                tvDate.text  = selectedDate
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).also { it.datePicker.minDate = cal.timeInMillis }.show()
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val amPm  = if (hour < 12) "AM" else "PM"
                val hour12 = when {
                    hour == 0  -> 12
                    hour > 12  -> hour - 12
                    else       -> hour
                }
                selectedTime = String.format("%d:%02d %s", hour12, minute, amPm)
                tvTime.text  = selectedTime
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnBook.isEnabled      = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}