package com.example.authapp.presentation.vets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.authapp.domain.repository.VetRepository
import com.example.authapp.model.Vet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class FindVetsViewModel @Inject constructor(
    private val vetRepository: VetRepository
) : ViewModel() {

    sealed class UiState {

        object Idle :
            UiState()

        object Loading :
            UiState()

        data class Success(
            val vets: List<Vet>
        ) : UiState()

        data class Error(
            val message: String
        ) : UiState()
    }


    private val _uiState =
        MutableStateFlow<UiState>(
            UiState.Idle
        )

    val uiState:
            StateFlow<UiState> =
        _uiState


    /*
     * Full list is kept here so filtering
     * remains client-side and simple.
     */
    private var allVets:
            List<Vet> =
        emptyList()


    fun loadAllVets() {

        viewModelScope.launch {

            _uiState.value =
                UiState.Loading


            vetRepository
                .getAllVets()
                .onSuccess { vets ->

                    allVets =
                        vets

                    _uiState.value =
                        UiState.Success(
                            vets
                        )
                }
                .onFailure { error ->

                    _uiState.value =
                        UiState.Error(
                            error.message
                                ?: "Failed to load veterinarians"
                        )
                }
        }
    }


    fun filterVets(
        query: String = "",
        city: String = "All",
        specialization: String = "All"
    ) {

        val cleanQuery =
            query.trim()


        val filtered =
            allVets.filter { vet ->

                val matchesQuery =
                    cleanQuery.isBlank() ||

                            vet.displayName.contains(
                                cleanQuery,
                                ignoreCase = true
                            ) ||

                            vet.clinicName.contains(
                                cleanQuery,
                                ignoreCase = true
                            ) ||

                            vet.city.contains(
                                cleanQuery,
                                ignoreCase = true
                            ) ||

                            vet.specialization.contains(
                                cleanQuery,
                                ignoreCase = true
                            )


                val matchesCity =
                    city == "All" ||

                            vet.city
                                .trim()
                                .equals(
                                    city.trim(),
                                    ignoreCase = true
                                )


                val matchesSpecialization =
                    specialization == "All" ||

                            vet.specialization
                                .trim()
                                .equals(
                                    specialization.trim(),
                                    ignoreCase = true
                                )


                matchesQuery &&
                        matchesCity &&
                        matchesSpecialization
            }


        _uiState.value =
            UiState.Success(
                filtered
            )
    }


    fun getAvailableCities():
            List<String> {

        return allVets
            .map { vet ->
                vet.city.trim()
            }
            .filter { city ->
                city.isNotBlank()
            }
            .distinctBy { city ->
                city.lowercase()
            }
            .sortedBy { city ->
                city.lowercase()
            }
    }


    fun getAvailableSpecializations():
            List<String> {

        return allVets
            .map { vet ->
                vet.specialization.trim()
            }
            .filter { specialization ->
                specialization.isNotBlank()
            }
            .distinctBy { specialization ->
                specialization.lowercase()
            }
            .sortedBy { specialization ->
                specialization.lowercase()
            }
    }


    fun getVetById(
        uid: String
    ): Vet? {

        return allVets.find { vet ->
            vet.uid == uid
        }
    }
}