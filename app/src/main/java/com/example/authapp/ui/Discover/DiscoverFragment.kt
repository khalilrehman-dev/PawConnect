package com.example.authapp.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Pet
import com.example.authapp.presentation.discover.DiscoverUiState
import com.example.authapp.presentation.discover.DiscoverViewModel
import com.example.authapp.ui.Vets.FindVetsActivity
import com.example.authapp.ui.pets.PetDetailActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DiscoverFragment :
    Fragment(R.layout.fragment_discover) {

    private val viewModel:
            DiscoverViewModel by viewModels()


    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            DiscoverAdapter


    private lateinit var searchView:
            SearchView

    private lateinit var chipGroup:
            ChipGroup


    private lateinit var progressDiscover:
            CircularProgressIndicator


    private lateinit var layoutEmpty:
            LinearLayout

    private lateinit var tvEmptyMessage:
            TextView


    private lateinit var layoutError:
            LinearLayout

    private lateinit var tvDiscoverError:
            TextView

    private lateinit var btnRetry:
            MaterialButton


    private lateinit var btnFindVet:
            MaterialButton


    private var selectedSpecies =
        "All"

    private var currentQuery =
        ""


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

        setupSearch()

        setupChips()

        setupClicks()

        observeViewModel()

        viewModel.loadAllPets()
    }


    private fun bindViews(
        view: View
    ) {

        recyclerView =
            view.findViewById(
                R.id.recyclerDiscoverPets
            )

        searchView =
            view.findViewById(
                R.id.searchDiscover
            )

        chipGroup =
            view.findViewById(
                R.id.chipGroupSpecies
            )


        progressDiscover =
            view.findViewById(
                R.id.progressDiscover
            )


        layoutEmpty =
            view.findViewById(
                R.id.layoutDiscoverEmpty
            )

        tvEmptyMessage =
            view.findViewById(
                R.id.tvDiscoverEmptyMessage
            )


        layoutError =
            view.findViewById(
                R.id.layoutDiscoverError
            )

        tvDiscoverError =
            view.findViewById(
                R.id.tvDiscoverError
            )

        btnRetry =
            view.findViewById(
                R.id.btnRetryDiscover
            )


        btnFindVet =
            view.findViewById(
                R.id.btnFindVet
            )
    }


    private fun setupRecyclerView() {

        adapter =
            DiscoverAdapter { pet ->

                openPetDetail(
                    pet
                )
            }


        recyclerView.layoutManager =
            GridLayoutManager(
                requireContext(),
                2
            )

        recyclerView.adapter =
            adapter
    }


    private fun setupSearch() {

        searchView.setOnQueryTextListener(
            object :
                SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {

                    return false
                }


                override fun onQueryTextChange(
                    newText: String?
                ): Boolean {

                    currentQuery =
                        newText.orEmpty()


                    applyFilters()

                    return true
                }
            }
        )
    }


    private fun setupChips() {

        val species =
            listOf(
                "All",
                "Dog",
                "Cat",
                "Bird",
                "Rabbit",
                "Other"
            )


        species.forEach { name ->

            val chip =
                Chip(requireContext()).apply {

                    text =
                        name

                    isCheckable =
                        true

                    isChecked =
                        name == "All"


                    setOnClickListener {

                        selectedSpecies =
                            name

                        applyFilters()
                    }
                }


            chipGroup.addView(
                chip
            )
        }
    }


    private fun setupClicks() {

        btnFindVet.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    FindVetsActivity::class.java
                )
            )
        }


        btnRetry.setOnClickListener {

            viewModel.loadAllPets()
        }
    }


    private fun applyFilters() {

        viewModel.filter(
            species =
                selectedSpecies,

            query =
                currentQuery
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

                                    DiscoverUiState.Idle -> {
                                        // Initial loader remains visible.
                                    }


                                    DiscoverUiState.Loading -> {

                                        showLoading()
                                    }


                                    is DiscoverUiState.Success -> {

                                        showPets(
                                            state
                                        )
                                    }


                                    DiscoverUiState.Empty -> {

                                        showEmpty()
                                    }


                                    is DiscoverUiState.Error -> {

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

        progressDiscover.visibility =
            View.VISIBLE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE
    }


    private fun showPets(
        state: DiscoverUiState.Success
    ) {

        progressDiscover.visibility =
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
    }


    private fun showEmpty() {

        progressDiscover.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        tvEmptyMessage.text =
            if (
                currentQuery.isNotBlank() ||
                selectedSpecies != "All"
            ) {

                "No pets match your current search."

            } else {

                "No other pet profiles are available right now."
            }


        layoutEmpty.visibility =
            View.VISIBLE
    }


    private fun showError(
        message: String
    ) {

        progressDiscover.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE


        tvDiscoverError.text =
            message

        layoutError.visibility =
            View.VISIBLE
    }


    private fun openPetDetail(
        pet: Pet
    ) {

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
}