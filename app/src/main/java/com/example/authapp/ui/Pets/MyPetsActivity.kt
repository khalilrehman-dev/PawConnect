package com.example.authapp.ui.pets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.authapp.R
import com.example.authapp.presentation.pets.PetsUiState
import com.example.authapp.presentation.pets.PetViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyPetsActivity : AppCompatActivity() {

    private val viewModel: PetViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_pets)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar  = findViewById(R.id.progressBar)
        tvEmpty      = findViewById(R.id.tvEmpty)

        adapter = PetAdapter { pet ->
            startActivity(
                Intent(this, PetDetailActivity::class.java)
                    .putExtra("petId",    pet.id)
                    .putExtra("petName",  pet.name)
                    .putExtra("petSpecies", pet.species)
                    .putExtra("petBreed", pet.breed)
                    .putExtra("petAge",   pet.age)
                    .putExtra("petGender", pet.gender)
                    .putExtra("petDesc",  pet.description)
                    .putExtra("petImage", pet.imageUrl)
                    .putExtra("ownerId",  pet.ownerId)
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddPet).setOnClickListener {
            startActivity(Intent(this, AddPetActivity::class.java))
        }

        supportActionBar?.apply {
            title = "My Pets"
            setDisplayHomeAsUpEnabled(true)
        }

        observeViewModel()
        viewModel.loadMyPets()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when coming back from AddPet or PetDetail
        viewModel.loadMyPets()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.petsState.collect { state ->
                    when (state) {
                        is PetsUiState.Idle    -> { }
                        is PetsUiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            tvEmpty.visibility     = View.GONE
                            recyclerView.visibility = View.GONE
                        }
                        is PetsUiState.Empty   -> {
                            progressBar.visibility  = View.GONE
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility      = View.VISIBLE
                        }
                        is PetsUiState.Success -> {
                            progressBar.visibility  = View.GONE
                            tvEmpty.visibility      = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            adapter.submitList(state.pets)
                        }
                        is PetsUiState.Error -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MyPetsActivity, state.message, Toast.LENGTH_LONG).show()
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
