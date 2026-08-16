package com.example.authapp.ui.Appointments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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

    private var vetId: String = ""
    private var vetName: String = ""
    private var clinicName: String = ""

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private var petList: List<Pet> = emptyList()

    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null
    private var selectedDay: Int? = null

    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        vetId =
            intent.getStringExtra("vetId") ?: ""

        vetName =
            intent.getStringExtra("vetName") ?: ""

        clinicName =
            intent.getStringExtra("clinic") ?: ""

        bindViews()
        observeViewModel()

        viewModel.loadMyPets()

        tvVetName.text =
            "Dr. $vetName"

        tvClinicName.text =
            clinicName

        tvDate.setOnClickListener {
            showDatePicker()
        }

        tvTime.setOnClickListener {
            showTimePicker()
        }

        btnBook.setOnClickListener {

            val selectedPet =
                petList.getOrNull(
                    spinnerPet.selectedItemPosition
                )

            if (selectedPet == null) {
                Toast.makeText(
                    this,
                    "Please add a pet first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val scheduledAt =
                getSelectedScheduledAt()

            if (scheduledAt == null) {
                Toast.makeText(
                    this,
                    "Select appointment date and time",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (scheduledAt <= System.currentTimeMillis()) {
                Toast.makeText(
                    this,
                    "Please select a future appointment time",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.bookAppointment(
                vetId = vetId,
                selectedPetId = selectedPet.id,
                date = selectedDate,
                time = selectedTime,
                scheduledAt = scheduledAt,
                note = etNote.text.toString()
            )
        }

        supportActionBar?.apply {
            title = "Book Appointment"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun bindViews() {

        tvVetName =
            findViewById(R.id.tvVetName)

        tvClinicName =
            findViewById(R.id.tvClinicName)

        spinnerPet =
            findViewById(R.id.spinnerPet)

        tvDate =
            findViewById(R.id.tvDate)

        tvTime =
            findViewById(R.id.tvTime)

        etNote =
            findViewById(R.id.etNote)

        btnBook =
            findViewById(R.id.btnBook)

        progressBar =
            findViewById(R.id.progressBar)
    }

    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // Pets
                launch {

                    viewModel.petsState.collect { pets ->

                        petList = pets

                        val names =
                            if (pets.isEmpty()) {
                                listOf("No pets added yet")
                            } else {
                                pets.map { it.name }
                            }

                        spinnerPet.adapter =
                            ArrayAdapter(
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

                            is BookingState.Idle -> {
                                showLoading(false)
                            }

                            is BookingState.Loading -> {
                                showLoading(true)
                            }

                            is BookingState.Success -> {
                                showLoading(false)
                            }

                            is BookingState.Error -> {

                                showLoading(false)

                                Toast.makeText(
                                    this@BookAppointmentActivity,
                                    state.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                // One-time events
                launch {

                    viewModel.events.collect { event ->

                        when (event) {

                            AppointmentEvent.BookingSuccess -> {

                                Toast.makeText(
                                    this@BookAppointmentActivity,
                                    "Appointment booked!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                finish()
                            }

                            is AppointmentEvent.Error -> {

                                Toast.makeText(
                                    this@BookAppointmentActivity,
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

    private fun showDatePicker() {

        val calendar =
            Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->

                selectedYear = year
                selectedMonth = month
                selectedDay = day

                selectedDate =
                    "$day/${month + 1}/$year"

                tvDate.text =
                    selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).also {

            it.datePicker.minDate =
                calendar.timeInMillis

        }.show()
    }

    private fun showTimePicker() {

        val calendar =
            Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hour, minute ->

                // IMPORTANT:
                // save actual 24-hour values for scheduledAt
                selectedHour = hour
                selectedMinute = minute

                val amPm =
                    if (hour < 12) {
                        "AM"
                    } else {
                        "PM"
                    }

                val hour12 =
                    when {
                        hour == 0 -> 12
                        hour > 12 -> hour - 12
                        else -> hour
                    }

                selectedTime =
                    String.format(
                        "%d:%02d %s",
                        hour12,
                        minute,
                        amPm
                    )

                tvTime.text =
                    selectedTime
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun getSelectedScheduledAt(): Long? {

        val year =
            selectedYear ?: return null

        val month =
            selectedMonth ?: return null

        val day =
            selectedDay ?: return null

        val hour =
            selectedHour ?: return null

        val minute =
            selectedMinute ?: return null

        return Calendar.getInstance().apply {

            set(
                Calendar.YEAR,
                year
            )

            set(
                Calendar.MONTH,
                month
            )

            set(
                Calendar.DAY_OF_MONTH,
                day
            )

            set(
                Calendar.HOUR_OF_DAY,
                hour
            )

            set(
                Calendar.MINUTE,
                minute
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )

        }.timeInMillis
    }

    private fun showLoading(show: Boolean) {

        progressBar.visibility =
            if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }

        btnBook.isEnabled =
            !show
    }

    override fun onSupportNavigateUp(): Boolean {

        onBackPressedDispatcher.onBackPressed()

        return true
    }
}