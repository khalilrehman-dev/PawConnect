package com.example.authapp.presentation.pets

import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.domain.repository.PetRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue

/**
 * Unit tests for PetViewModel — specifically the validation logic
 * in addPet(), since that's where most of our business rules live.
 */
class PetViewModelTest {

    private lateinit var petRepository: PetRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: PetViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        petRepository  = mockk()
        authRepository = mockk()

        // Every test needs a logged in user, so we fake that here
        every { authRepository.getCurrentUid() } returns "fakeUid123"

        viewModel = PetViewModel(petRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `adding pet with empty name shows error`() {
        viewModel.addPet(
            name = "",
            species = "Dog",
            breed = "Labrador",
            age = "3",
            gender = "Male",
            description = ""
        )

        val state = viewModel.actionState.value
        assertTrue(state is PetActionState.Error)
    }

    @Test
    fun `adding pet without species shows error`() {
        viewModel.addPet(
            name = "Bruno",
            species = "",
            breed = "Labrador",
            age = "3",
            gender = "Male",
            description = ""
        )

        val state = viewModel.actionState.value
        assertTrue(state is PetActionState.Error)
    }

    @Test
    fun `adding pet with invalid age shows error`() {
        viewModel.addPet(
            name = "Bruno",
            species = "Dog",
            breed = "Labrador",
            age = "notanumber",
            gender = "Male",
            description = ""
        )

        val state = viewModel.actionState.value
        assertTrue(state is PetActionState.Error)
    }

    @Test
    fun `adding pet without photo shows error`() {
        // No setImage() called — pendingImageBytes stays null
        viewModel.addPet(
            name = "Bruno",
            species = "Dog",
            breed = "Labrador",
            age = "3",
            gender = "Male",
            description = ""
        )

        val state = viewModel.actionState.value
        assertTrue(state is PetActionState.Error)
    }
}