package com.example.authapp.ui.Vets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Vet
import com.example.authapp.presentation.vets.FindVetsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class FindVetsActivity :
    AppCompatActivity() {

    private val viewModel:
            FindVetsViewModel by viewModels()


    private lateinit var toolbar:
            MaterialToolbar

    private lateinit var recyclerView:
            RecyclerView

    private lateinit var adapter:
            VetAdapter


    private lateinit var searchView:
            SearchView

    private lateinit var chipGroupCity:
            ChipGroup

    private lateinit var chipGroupSpec:
            ChipGroup


    private lateinit var progressBar:
            CircularProgressIndicator


    private lateinit var layoutEmpty:
            LinearLayout

    private lateinit var tvEmpty:
            TextView


    private lateinit var layoutError:
            LinearLayout

    private lateinit var tvVetError:
            TextView

    private lateinit var btnRetry:
            MaterialButton


    private var selectedCity =
        "All"

    private var selectedSpec =
        "All"

    private var currentQuery =
        ""


    private var filtersInitialized =
        false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        enableEdgeToEdge()

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_find_vets
        )


        bindViews()

        applySystemInsets()

        setupToolbar()

        setupRecyclerView()

        setupSearch()

        setupClicks()

        observeViewModel()


        viewModel.loadAllVets()
    }


    private fun bindViews() {

        toolbar =
            findViewById(
                R.id.toolbarFindVets
            )


        recyclerView =
            findViewById(
                R.id.recyclerView
            )


        searchView =
            findViewById(
                R.id.searchView
            )


        chipGroupCity =
            findViewById(
                R.id.chipGroupCity
            )

        chipGroupSpec =
            findViewById(
                R.id.chipGroupSpec
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )


        layoutEmpty =
            findViewById(
                R.id.layoutEmptyVets
            )

        tvEmpty =
            findViewById(
                R.id.tvEmpty
            )


        layoutError =
            findViewById(
                R.id.layoutVetError
            )

        tvVetError =
            findViewById(
                R.id.tvVetError
            )

        btnRetry =
            findViewById(
                R.id.btnRetryVets
            )
    }


    private fun applySystemInsets() {

        val root =
            findViewById<View>(
                R.id.rootFindVets
            )


        ViewCompat
            .setOnApplyWindowInsetsListener(
                root
            ) { view, insets ->

                val systemBars =
                    insets.getInsets(
                        WindowInsetsCompat
                            .Type
                            .systemBars()
                    )


                view.setPadding(
                    0,
                    systemBars.top,
                    0,
                    systemBars.bottom
                )


                insets
            }
    }


    private fun setupToolbar() {

        setSupportActionBar(
            toolbar
        )


        supportActionBar?.apply {

            title =
                "Find Veterinarians"

            setDisplayHomeAsUpEnabled(
                true
            )
        }
    }


    private fun setupRecyclerView() {

        adapter =
            VetAdapter { vet ->

                openVetDetail(
                    vet
                )
            }


        recyclerView.layoutManager =
            LinearLayoutManager(
                this
            )


        recyclerView.adapter =
            adapter
    }


    private fun setupSearch() {

        searchView
            .setOnQueryTextListener(

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


    private fun setupClicks() {

        btnRetry.setOnClickListener {

            filtersInitialized =
                false

            selectedCity =
                "All"

            selectedSpec =
                "All"

            currentQuery =
                searchView.query
                    ?.toString()
                    .orEmpty()


            viewModel.loadAllVets()
        }
    }


    private fun setupFilterChips() {

        chipGroupCity.removeAllViews()

        chipGroupSpec.removeAllViews()


        addCityChip(
            "All"
        )


        viewModel
            .getAvailableCities()
            .forEach { city ->

                addCityChip(
                    city
                )
            }


        addSpecializationChip(
            "All"
        )


        viewModel
            .getAvailableSpecializations()
            .forEach { specialization ->

                addSpecializationChip(
                    specialization
                )
            }


        filtersInitialized =
            true
    }


    private fun addCityChip(
        city: String
    ) {

        val chip =
            Chip(this).apply {

                text =
                    city

                isCheckable =
                    true

                isChecked =
                    city == selectedCity


                setOnClickListener {

                    selectedCity =
                        city

                    applyFilters()
                }
            }


        chipGroupCity.addView(
            chip
        )
    }


    private fun addSpecializationChip(
        specialization: String
    ) {

        val chip =
            Chip(this).apply {

                text =
                    specialization

                isCheckable =
                    true

                isChecked =
                    specialization ==
                            selectedSpec


                setOnClickListener {

                    selectedSpec =
                        specialization

                    applyFilters()
                }
            }


        chipGroupSpec.addView(
            chip
        )
    }


    private fun applyFilters() {

        viewModel.filterVets(
            query =
                currentQuery,

            city =
                selectedCity,

            specialization =
                selectedSpec
        )
    }


    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState
                    .collect { state ->

                        when (state) {

                            FindVetsViewModel
                                .UiState
                                .Idle -> {
                                // Initial loader is already visible.
                            }


                            FindVetsViewModel
                                .UiState
                                .Loading -> {

                                showLoading()
                            }


                            is FindVetsViewModel
                            .UiState
                            .Success -> {

                                if (!filtersInitialized) {

                                    setupFilterChips()
                                }


                                if (state.vets.isEmpty()) {

                                    showEmpty()

                                } else {

                                    showVets(
                                        state.vets
                                    )
                                }
                            }


                            is FindVetsViewModel
                            .UiState
                            .Error -> {

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

        progressBar.visibility =
            View.VISIBLE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE
    }


    private fun showVets(
        vets: List<Vet>
    ) {

        progressBar.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        adapter.submitList(
            vets
        )


        recyclerView.visibility =
            View.VISIBLE
    }


    private fun showEmpty() {

        progressBar.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutError.visibility =
            View.GONE


        tvEmpty.text =
            if (
                currentQuery.isNotBlank() ||
                selectedCity != "All" ||
                selectedSpec != "All"
            ) {

                "Try changing your search or filters."

            } else {

                "No veterinarian profiles are available right now."
            }


        layoutEmpty.visibility =
            View.VISIBLE
    }


    private fun showError(
        message: String
    ) {

        progressBar.visibility =
            View.GONE

        recyclerView.visibility =
            View.GONE

        layoutEmpty.visibility =
            View.GONE


        tvVetError.text =
            message

        layoutError.visibility =
            View.VISIBLE
    }


    private fun openVetDetail(
        vet: Vet
    ) {

        startActivity(
            Intent(
                this,
                VetDetailActivity::class.java
            ).apply {

                putExtra(
                    "vetUid",
                    vet.uid
                )

                putExtra(
                    "vetName",
                    vet.displayName
                )

                putExtra(
                    "clinic",
                    vet.clinicName
                )

                putExtra(
                    "city",
                    vet.city
                )

                putExtra(
                    "address",
                    vet.address
                )

                putExtra(
                    "phone",
                    vet.phoneNumber
                )

                putExtra(
                    "spec",
                    vet.specialization
                )

                putExtra(
                    "years",
                    vet.yearsOfExperience
                )

                putExtra(
                    "imageUrl",
                    vet.profileImageUrl
                )
            }
        )
    }


    override fun onSupportNavigateUp():
            Boolean {

        onBackPressedDispatcher
            .onBackPressed()

        return true
    }
}