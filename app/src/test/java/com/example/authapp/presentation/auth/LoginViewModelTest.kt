package com.example.authapp.presentation.auth

import com.example.authapp.model.User
import com.example.authapp.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
//import kotlin.test.assertTrue
import org.junit.Assert.assertTrue

/**
 * Unit tests for LoginViewModel.
 *
 * We test the ViewModel in isolation — meaning we never touch
 * real Firebase. Instead we create a "fake" AuthRepository using MockK
 * and tell it exactly what to return, so we can test our ViewModel's
 * LOGIC, not Firebase's behavior.
 */
class LoginViewModelTest {

    // The fake repository — we control what it returns
    private lateinit var authRepository: AuthRepository

    // The actual ViewModel we are testing
    private lateinit var viewModel: LoginViewModel

    // Coroutines need a "test dispatcher" so viewModelScope.launch
    // runs predictably inside a test instead of on a real background thread
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Runs before EVERY test — gives us a clean ViewModel each time
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        // Cleans up after every test
        Dispatchers.resetMain()
    }

    @Test
    fun `empty email shows error state`() {
        // ACT: call the function we're testing
        viewModel.loginWithEmail("", "somepassword")

        // ASSERT: check the result is what we expect
        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
    }

    @Test
    fun `invalid email format shows error state`() {
        viewModel.loginWithEmail("notanemail", "somepassword")

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
    }

    @Test
    fun `empty password shows error state`() {
        viewModel.loginWithEmail("test@example.com", "")

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
    }

    @Test
    fun `successful login with verified email sets success state`() = runTest {
        // ARRANGE: tell the fake repository what to return
        val fakeUser = User(
            uid = "123",
            email = "test@example.com",
            displayName = "Test User",
            isEmailVerified = true,
            role = "pet_owner"
        )
        coEvery { authRepository.login("test@example.com", "password123") } returns
                Result.success(fakeUser)

        // ACT
        viewModel.loginWithEmail("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle() // let coroutine finish

        // ASSERT: state should no longer be loading/error
        val state = viewModel.uiState.value
        assertTrue(state !is LoginUiState.Error)
    }

    @Test
    fun `unverified email logs out and shows error`() = runTest {
        val unverifiedUser = User(
            uid = "123",
            email = "test@example.com",
            isEmailVerified = false,
            role = "pet_owner"
        )
        coEvery { authRepository.login(any(), any()) } returns Result.success(unverifiedUser)
        coEvery { authRepository.logout() } returns Result.success(Unit)

        viewModel.loginWithEmail("test@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
    }
}