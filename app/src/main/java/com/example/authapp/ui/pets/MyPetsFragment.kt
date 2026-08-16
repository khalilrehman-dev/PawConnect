package com.example.authapp.ui.pets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.presentation.pets.PetViewModel
import com.example.authapp.presentation.pets.PetsUiState
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MyPetsFragment :
    Fragment(R.layout.fragment_my_pets) {

    private val viewModel:
            PetViewModel by viewModels()


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            PetAdapter


    private lateinit var progressPets:
            CircularProgressIndicator


    private lateinit var layoutEmpty:
            LinearLayout

    private lateinit var btnEmptyAddPet:
            MaterialButton


    private lateinit var layoutError:
            LinearLayout

    private lateinit var tvPetsError:
            TextView

    private lateinit var btnRetryPets:
            MaterialButton


    private lateinit var fabAddPet:
            ExtendedFloatingActionButton


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

        setupClicks()

        observePets()
    }


    override fun onResume() {
        super.onResume()

        /*
         * Refresh after returning from
         * AddPetActivity or PetDetailActivity.
         */
        viewModel.loadMyPets()
    }


    private fun bindViews(
        view: View
    ) {

        recyclerView =
            view.findViewById(
                R.id.recyclerPets
            )


        progressPets =
            view.findViewById(
                R.id.progressPets
            )


        layoutEmpty =
            view.findViewById(
                R.id.layoutEmptyPets
            )

        btnEmptyAddPet =
            view.findViewById(
                R.id.btnEmptyAddPet
            )


        layoutError =
            view.findViewById(
                R.id.layoutPetsError
            )

        tvPetsError =
            view.findViewById(
                R.id.tvPetsError
            )

        btnRetryPets =
            view.findViewById(
                R.id.btnRetryPets
            )


        fabAddPet =
            view.findViewById(
                R.id.fabAddPet
            )
    }


    private fun setupRecyclerView() {

        adapter =
            PetAdapter { pet ->

                startActivity(
                    Intent(
                        requireContext(),
                        PetDetailActivity::class.java
                    ).apply {

                        putExtra(
                            "petId",
                            pet.id
                        )

                        putExtra(
                            "petName",
                            pet.name
                        )

                        putExtra(
                            "petSpecies",
                            pet.species
                        )

                        putExtra(
                            "petBreed",
                            pet.breed
                        )

                        putExtra(
                            "petAge",
                            pet.age
                        )

                        putExtra(
                            "petGender",
                            pet.gender
                        )

                        putExtra(
                            "petDesc",
                            pet.description
                        )

                        putExtra(
                            "petImage",
                            pet.imageUrl
                        )

                        putExtra(
                            "ownerId",
                            pet.ownerId
                        )
                    }
                )
            }


        recyclerView.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        recyclerView.adapter =
            adapter
    }


    private fun setupClicks() {

        fabAddPet.setOnClickListener {

            openAddPet()
        }


        btnEmptyAddPet.setOnClickListener {

            openAddPet()
        }


        btnRetryPets.setOnClickListener {

            viewModel.loadMyPets()
        }
    }


    private fun openAddPet() {

        startActivity(
            Intent(
                requireContext(),
                AddPetActivity::class.java
            )
        )
    }


    private fun observePets() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        viewModel.petsState
                            .collect { state ->

                                when (state) {

                                    PetsUiState.Idle -> {
                                        // Initial loader remains visible.
                                    }


                                    PetsUiState.Loading -> {

                                        showLoading()
                                    }


                                    PetsUiState.Empty -> {

                                        showEmpty()
                                    }


                                    is PetsUiState.Success -> {

                                        showPets(
                                            state
                                        )
                                    }


                                    is PetsUiState.Error -> {

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

        progressPets.visibility =
            View.VISIBLE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE

        fabAddPet.visibility =
            View.GONE
    }


    private fun showEmpty() {

        progressPets.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.VISIBLE

        fabAddPet.visibility =
            View.GONE
    }


    private fun showPets(
        state: PetsUiState.Success
    ) {

        progressPets.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        adapter.submitList(
            state.pets
        )


        recyclerView.visibility =
            View.VISIBLE

        fabAddPet.visibility =
            View.VISIBLE
    }


    private fun showError(
        message: String
    ) {

        progressPets.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        fabAddPet.visibility =
            View.GONE


        tvPetsError.text =
            message

        layoutError.visibility =
            View.VISIBLE
    }
}