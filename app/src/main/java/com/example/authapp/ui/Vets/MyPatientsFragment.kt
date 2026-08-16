package com.example.authapp.ui.Vets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.authapp.R
import com.example.authapp.presentation.vets.PatientUiItem
import com.example.authapp.presentation.vets.PatientsUiState
import com.example.authapp.presentation.vets.PatientsViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MyPatientsFragment :
    Fragment(R.layout.fragment_my_patients) {

    private val viewModel:
            PatientsViewModel by viewModels()


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            PatientFragmentAdapter


    private lateinit var layoutLoading:
            View

    private lateinit var layoutEmpty:
            View

    private lateinit var layoutError:
            View


    private lateinit var tvPatientsError:
            TextView

    private lateinit var btnRetry:
            MaterialButton


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )


        bindViews(
            view
        )

        setupRecyclerView()

        setupClicks()

        observeViewModel()
    }


    override fun onResume() {
        super.onResume()

        /*
         * Refresh the patient list whenever
         * the veterinarian returns to this tab.
         */
        viewModel.loadPatients()
    }


    private fun bindViews(
        view: View
    ) {

        recyclerView =
            view.findViewById(
                R.id.recyclerPatients
            )


        layoutLoading =
            view.findViewById(
                R.id.layoutPatientsLoading
            )


        layoutEmpty =
            view.findViewById(
                R.id.layoutPatientsEmpty
            )


        layoutError =
            view.findViewById(
                R.id.layoutPatientsError
            )


        tvPatientsError =
            view.findViewById(
                R.id.tvPatientsError
            )


        btnRetry =
            view.findViewById(
                R.id.btnRetryPatients
            )
    }


    private fun setupRecyclerView() {

        adapter =
            PatientFragmentAdapter()


        recyclerView.layoutManager =
            LinearLayoutManager(
                requireContext()
            )


        recyclerView.adapter =
            adapter
    }


    private fun setupClicks() {

        btnRetry.setOnClickListener {

            viewModel.loadPatients()
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

                        viewModel
                            .uiState
                            .collect { state ->

                                when (state) {

                                    PatientsUiState.Loading -> {

                                        showLoading()
                                    }


                                    is PatientsUiState.Success -> {

                                        showPatients(
                                            state.patients
                                        )
                                    }


                                    PatientsUiState.Empty -> {

                                        showEmpty()
                                    }


                                    is PatientsUiState.Error -> {

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

        layoutLoading.visibility =
            View.VISIBLE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE
    }


    private fun showPatients(
        patients: List<PatientUiItem>
    ) {

        layoutLoading.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        adapter.submitList(
            patients
        )


        recyclerView.visibility =
            View.VISIBLE
    }


    private fun showEmpty() {

        layoutLoading.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        layoutEmpty.visibility =
            View.VISIBLE
    }


    private fun showError(
        message: String
    ) {

        layoutLoading.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE


        tvPatientsError.text =
            message


        layoutError.visibility =
            View.VISIBLE
    }
}


/*
 * Simple RecyclerView adapter for the Vet Patients tab.
 *
 * During the current migration it stays in the same
 * Kotlin file so the screen remains easy to understand.
 */
class PatientFragmentAdapter :
    RecyclerView.Adapter<
            PatientFragmentAdapter.ViewHolder
            >() {

    private val items =
        mutableListOf<PatientUiItem>()


    fun submitList(
        patients: List<PatientUiItem>
    ) {

        items.clear()

        items.addAll(
            patients
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
                    R.layout.item_patient,
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

        private val ivPetImage:
                ImageView =
            itemView.findViewById(
                R.id.ivPatientPetImage
            )


        private val tvPetName:
                TextView =
            itemView.findViewById(
                R.id.tvPetName
            )


        private val tvOwnerName:
                TextView =
            itemView.findViewById(
                R.id.tvOwnerName
            )


        private val tvPetDetails:
                TextView =
            itemView.findViewById(
                R.id.tvPatientPetDetails
            )


        private val tvVisitLabel:
                TextView =
            itemView.findViewById(
                R.id.tvPatientVisitLabel
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


        fun bind(
            item: PatientUiItem
        ) {

            val pet =
                item.pet


            val appointment =
                item.appointment


            bindPetName(
                item
            )

            bindOwner(
                item
            )

            bindPetDetails(
                item
            )

            bindVisit(
                item
            )

            bindPetImage(
                item
            )
        }


        private fun bindPetName(
            item: PatientUiItem
        ) {

            tvPetName.text =
                item.pet.name
                    .ifBlank {

                        "Patient"
                    }
        }


        private fun bindOwner(
            item: PatientUiItem
        ) {

            val ownerName =
                item.ownerName
                    .trim()
                    .ifBlank {

                        "Pet Owner"
                    }


            tvOwnerName.text =
                "Owner: $ownerName"
        }


        private fun bindPetDetails(
            item: PatientUiItem
        ) {

            val pet =
                item.pet


            tvPetDetails.text =
                buildString {

                    if (
                        pet.species.isNotBlank()
                    ) {

                        append(
                            pet.species
                        )
                    }


                    if (
                        pet.breed.isNotBlank()
                    ) {

                        if (
                            isNotEmpty()
                        ) {

                            append(
                                " • "
                            )
                        }


                        append(
                            pet.breed
                        )
                    }


                    if (
                        pet.age > 0
                    ) {

                        if (
                            isNotEmpty()
                        ) {

                            append(
                                " • "
                            )
                        }


                        append(
                            pet.age
                        )


                        append(
                            if (
                                pet.age == 1
                            ) {

                                " yr"

                            } else {

                                " yrs"
                            }
                        )
                    }


                    if (
                        pet.gender.isNotBlank()
                    ) {

                        if (
                            isNotEmpty()
                        ) {

                            append(
                                " • "
                            )
                        }


                        append(
                            pet.gender
                        )
                    }
                }
                    .ifBlank {

                        "Pet profile"
                    }
        }


        private fun bindVisit(
            item: PatientUiItem
        ) {

            val appointment =
                item.appointment


            tvVisitLabel.text =
                if (
                    appointment.scheduledAt >
                    System.currentTimeMillis()
                ) {

                    "Upcoming visit"

                } else {

                    "Latest accepted visit"
                }


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


        private fun bindPetImage(
            item: PatientUiItem
        ) {

            val imageUrl =
                item.pet.imageUrl


            if (
                imageUrl.isBlank()
            ) {

                ivPetImage.setImageResource(
                    R.drawable.ic_pet_placeholder
                )


                return
            }


            ivPetImage.load(
                imageUrl
            ) {

                crossfade(
                    true
                )


                placeholder(
                    R.drawable.ic_pet_placeholder
                )


                error(
                    R.drawable.ic_pet_placeholder
                )
            }
        }
    }
}