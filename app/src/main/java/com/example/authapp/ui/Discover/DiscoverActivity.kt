package com.example.authapp.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.model.Pet
import com.example.authapp.presentation.discover.DiscoverUiState
import com.example.authapp.presentation.discover.DiscoverViewModel
import com.example.authapp.ui.pets.PetDetailActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiscoverActivity : AppCompatActivity() {

    private val viewModel: DiscoverViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: DiscoverAdapter

    private var selectedSpecies = "All"
    private var currentQuery    = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover)

        bindViews()
        setupRecyclerView()
        setupSearch()
        setupChips()
        observeViewModel()

        supportActionBar?.apply {
            title = "Discover Pets"
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel.loadAllPets()
    }

    private fun bindViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        tvEmpty      = findViewById(R.id.tvEmpty)
        searchView   = findViewById(R.id.searchView)
        chipGroup    = findViewById(R.id.chipGroup)
    }

    private fun setupRecyclerView() {
        adapter = DiscoverAdapter { pet -> openPetDetail(pet) }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter       = adapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                viewModel.filter(selectedSpecies, currentQuery)
                return true
            }
        })
    }

    private fun setupChips() {
        val species = listOf("All", "Dog", "Cat", "Bird", "Rabbit", "Other")
        species.forEach { name ->
            val chip = Chip(this).apply {
                text         = name
                isCheckable  = true
                isChecked    = name == "All"
                setOnClickListener {
                    selectedSpecies = name
                    viewModel.filter(selectedSpecies, currentQuery)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DiscoverUiState.Idle    -> { }
                        is DiscoverUiState.Loading -> {
                            progressBar.visibility  = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility      = View.GONE
                        }
                        is DiscoverUiState.Success -> {
                            progressBar.visibility  = View.GONE
                            tvEmpty.visibility      = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            adapter.submitList(state.pets)
                        }
                        is DiscoverUiState.Empty   -> {
                            progressBar.visibility  = View.GONE
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility      = View.VISIBLE
                            tvEmpty.text            = "No pets found"
                        }
                        is DiscoverUiState.Error   -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@DiscoverActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun openPetDetail(pet: Pet) {
        startActivity(
            Intent(this, PetDetailActivity::class.java).apply {
                putExtra("petId",     pet.id)
                putExtra("petName",   pet.name)
                putExtra("petSpecies",pet.species)
                putExtra("petBreed",  pet.breed)
                putExtra("petAge",    pet.age)
                putExtra("petGender", pet.gender)
                putExtra("petDesc",   pet.description)
                putExtra("petImage",  pet.imageUrl)
                putExtra("ownerId",   pet.ownerId)
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
