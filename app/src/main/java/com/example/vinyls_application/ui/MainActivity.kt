package com.example.vinyls_application.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.ActivityMainBinding
import androidx.core.view.isVisible
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    // Destinations reachable from the NavigationWidget: they don't show the return arrow
    private val topLevelDestinations = setOf(
        R.id.loginFragment,
        R.id.navigation_albums,
        R.id.navigation_artists
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            getPreferences(MODE_PRIVATE).edit { clear() }
        }

        // Get the navigation host fragment from this Activity
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Instantiate the navController using the NavHostFragment
        navController = navHostFragment.navController
        // Make sure actions in the ActionBar get propagated to the NavController
Log.d("act", navController.toString())
        // Keep the HeaderWidget in sync with the current destination
        binding.headerWidget.setOnReturnClickListener { navController.navigateUp() }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.headerWidget.setContent(
                header = destination.label?.toString(),
                hasReturn = destination.id !in topLevelDestinations,
                hasLogo =  destination.id == R.id.loginFragment

            )
            binding.navigationWidget.isVisible = destination.id != R.id.loginFragment
        }
        // Bind the bottom NavigationWidget to the NavController
        binding.navigationWidget.setupWithNavController(navController)

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}