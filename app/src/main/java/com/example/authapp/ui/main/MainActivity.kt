package com.example.authapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.authapp.R
import com.example.authapp.domain.repository.AuthRepository
import com.example.authapp.ui.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository


    private lateinit var navController: NavController

    private lateinit var navHostView: View

    private lateinit var bottomNavigation:
            BottomNavigationView

    private lateinit var progressMain:
            CircularProgressIndicator

    private lateinit var layoutMainError:
            LinearLayout

    private lateinit var tvMainError:
            TextView

    private lateinit var btnMainRetry:
            MaterialButton


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        bindViews()

        setupNavigationController()

        setupActions()

        loadUserAndConfigureShell()
    }


    private fun bindViews() {

        navHostView =
            findViewById(
                R.id.navHostFragment
            )

        bottomNavigation =
            findViewById(
                R.id.bottomNavigation
            )

        progressMain =
            findViewById(
                R.id.progressMain
            )

        layoutMainError =
            findViewById(
                R.id.layoutMainError
            )

        tvMainError =
            findViewById(
                R.id.tvMainError
            )

        btnMainRetry =
            findViewById(
                R.id.btnMainRetry
            )
    }


    private fun setupNavigationController() {

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.navHostFragment
                ) as NavHostFragment

        navController =
            navHostFragment.navController
    }


    private fun setupActions() {

        btnMainRetry.setOnClickListener {

            loadUserAndConfigureShell()
        }
    }


    private fun loadUserAndConfigureShell() {

        val uid =
            authRepository.getCurrentUid()

        if (uid.isNullOrBlank()) {

            openWelcome()
            return
        }

        showLoading()

        lifecycleScope.launch {

            val result =
                authRepository
                    .getUserFromFirestore(
                        uid
                    )

            if (result.isFailure) {

                showError(
                    "We couldn't load your profile. " +
                            "Check your internet connection " +
                            "and try again."
                )

                return@launch
            }


            val user =
                result.getOrThrow()


            when (user.role) {

                ROLE_PET_OWNER -> {

                    configureShell(
                        graphResId =
                            R.navigation.nav_owner,

                        menuResId =
                            R.menu.menu_bottom_owner
                    )
                }


                ROLE_VETERINARIAN -> {

                    configureShell(
                        graphResId =
                            R.navigation.nav_vet,

                        menuResId =
                            R.menu.menu_bottom_vet
                    )
                }


                else -> {

                    showError(
                        "Your account role is unavailable. " +
                                "Please sign in again or contact support."
                    )
                }
            }
        }
    }


    private fun configureShell(
        graphResId: Int,
        menuResId: Int
    ) {

        /*
         * Avoid resetting the graph after
         * configuration restoration or retry.
         */
        val currentGraphId =
            runCatching {
                navController.graph.id
            }.getOrNull()


        if (currentGraphId != graphResId) {

            navController.setGraph(
                graphResId
            )
        }


        /*
         * Owner and Vet use different
         * five-item navigation menus.
         */
        bottomNavigation.menu.clear()

        bottomNavigation.inflateMenu(
            menuResId
        )


        /*
         * Menu item IDs match destination IDs,
         * so NavigationUI can control navigation
         * and selected-state automatically.
         */
        bottomNavigation
            .setupWithNavController(
                navController
            )


        progressMain.visibility =
            View.GONE

        layoutMainError.visibility =
            View.GONE

        navHostView.visibility =
            View.VISIBLE

        bottomNavigation.visibility =
            View.VISIBLE
    }


    private fun showLoading() {

        progressMain.visibility =
            View.VISIBLE

        layoutMainError.visibility =
            View.GONE

        navHostView.visibility =
            View.GONE

        bottomNavigation.visibility =
            View.GONE
    }


    private fun showError(
        message: String
    ) {

        progressMain.visibility =
            View.GONE

        navHostView.visibility =
            View.GONE

        bottomNavigation.visibility =
            View.GONE

        tvMainError.text =
            message

        layoutMainError.visibility =
            View.VISIBLE
    }


    private fun openWelcome() {

        startActivity(
            Intent(
                this,
                WelcomeActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }


    companion object {

        private const val ROLE_PET_OWNER =
            "pet_owner"

        private const val ROLE_VETERINARIAN =
            "veterinarian"
    }
}