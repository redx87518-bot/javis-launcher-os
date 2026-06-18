package com.javis.launcher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.javis.launcher.services.JavisForegroundService
import com.javis.launcher.ui.home.HomeScreen
import com.javis.launcher.ui.mission.MissionControlScreen
import com.javis.launcher.ui.onboarding.OnboardingScreen
import com.javis.launcher.ui.settings.SettingsScreen
import com.javis.launcher.ui.theme.JavisTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.SharedPreferences

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startJavisService()

        setContent {
            JavisTheme {
                val navController = rememberNavController()
                val hasCompletedOnboarding = prefs.getBoolean("onboarding_complete", false)
                val startDest = if (hasCompletedOnboarding) "home" else "onboarding"

                NavHost(navController = navController, startDestination = startDest) {
                    composable("onboarding") {
                        OnboardingScreen(navController = navController)
                    }
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                    composable("mission") {
                        MissionControlScreen(navController = navController)
                    }
                }
            }
        }
    }

    private fun startJavisService() {
        try {
            val serviceIntent = Intent(this, JavisForegroundService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
