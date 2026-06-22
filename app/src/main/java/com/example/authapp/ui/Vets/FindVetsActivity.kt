package com.example.authapp.ui.Vets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Vet
//import com.example.authapp.presentation.vets.FindVetsUiState
import com.example.authapp.presentation.vets.FindVetsViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FindVetsActivity : AppCompatActivity() {

    private val viewModel: FindVetsViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var searchView: SearchView
    private lateinit var chipGroupCity: ChipGroup
    private lateinit var chipGroupSpec: ChipGroup
    private lateinit var adapter: VetAdapter

    private var selectedCity = "All"
    private var selectedSpec = "All"
    private var currentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_vets)

        bindViews()
        setupRecyclerView()
        setupSearch()
        setupChips()
        observeViewModel()

        supportActionBar?.apply {
            title = "Find Vets"
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel.loadAllVets()
    }

    private fun bindViews() {
        recyclerView  = findViewById(R.id.recyclerView)
        progressBar   = findViewById(R.id.progressBar)
        tvEmpty       = findViewById(R.id.tvEmpty)
        searchView    = findViewById(R.id.searchView)
        chipGroupCity = findViewById(R.id.chipGroupCity)
        chipGroupSpec = findViewById(R.id.chipGroupSpec)
    }

    private fun setupRecyclerView() {
        adapter = VetAdapter { vet -> openVetDetail(vet) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                viewModel.filterVets(name = currentQuery, city = selectedCity, specialization = selectedSpec)
                return true
            }
        })
    }

    private fun setupChips() {
        listOf("All", "Islamabad", "Rawalpindi", "Lahore", "Karachi", "Peshawar", "Other")
            .forEach { city ->
                chipGroupCity.addView(Chip(this).apply {
                    text        = city
                    isCheckable = true
                    isChecked   = city == "All"
                    setOnClickListener {
                        selectedCity = city
                        viewModel.filterVets(name = currentQuery, city = selectedCity, specialization = selectedSpec)
                    }
                })
            }

        listOf("All", "General Practice", "Surgery", "Dermatology", "Dentistry", "Orthopedics", "Emergency Care")
            .forEach { spec ->
                chipGroupSpec.addView(Chip(this).apply {
                    text        = spec
                    isCheckable = true
                    isChecked   = spec == "All"
                    setOnClickListener {
                        selectedSpec = spec
                        viewModel.filterVets(name = currentQuery, city = selectedCity, specialization = selectedSpec)
                    }
                })
            }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is FindVetsViewModel.UiState.Idle    -> { }
                        is FindVetsViewModel.UiState.Loading -> {
                            progressBar.visibility  = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility      = View.GONE
                        }
                        is FindVetsViewModel.UiState.Success -> {
                            progressBar.visibility  = View.GONE
                            recyclerView.visibility = if (state.vets.isEmpty()) View.GONE else View.VISIBLE
                            tvEmpty.visibility      = if (state.vets.isEmpty()) View.VISIBLE else View.GONE
                            adapter.submitList(state.vets)
                        }
                        is FindVetsViewModel.UiState.Error -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@FindVetsActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun openVetDetail(vet: Vet) {
        startActivity(Intent(this, VetDetailActivity::class.java).apply {
            putExtra("vetUid",   vet.uid)
            putExtra("vetName",  vet.displayName)
            putExtra("clinic",   vet.clinicName)
            putExtra("city",     vet.city)
            putExtra("address",  vet.address)
            putExtra("phone",    vet.phoneNumber)
            putExtra("spec",     vet.specialization)
            putExtra("years",    vet.yearsOfExperience)
            putExtra("imageUrl", vet.profileImageUrl)
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}