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
import com.javis.launcher.ui.about.AboutScreen
import com.javis.launcher.ui.about.PrivacyScreen
import com.javis.launcher.ui.appdrawer.AppDrawerScreen
import com.javis.launcher.ui.appdrawer.AppIntelligenceScreen
import com.javis.launcher.ui.conversation.ConversationScreen
import com.javis.launcher.ui.home.HomeScreen
import com.javis.launcher.ui.memory.MemoryScreen
import com.javis.launcher.ui.mission.MissionControlScreen
import com.javis.launcher.ui.notifications.NotificationIntelligenceScreen
import com.javis.launcher.ui.onboarding.OnboardingScreen
import com.javis.launcher.ui.screen.ScreenUnderstandingScreen
import com.javis.launcher.ui.settings.SettingsScreen
import com.javis.launcher.ui.skills.SkillDetailScreen
import com.javis.launcher.ui.skills.SkillsScreen
import com.javis.launcher.ui.theme.JavisTheme
import com.javis.launcher.ui.voice.VoiceModeScreen
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
                    composable("conversation") {
                        ConversationScreen(navController = navController)
                    }
                    composable("memory") {
                        MemoryScreen(navController = navController)
                    }
                    composable("apps") {
                        AppDrawerScreen(navController = navController)
                    }
                    composable("skills") {
                        SkillsScreen(navController = navController)
                    }
                    composable("skill_detail/{skillId}") { backStackEntry ->
                        SkillDetailScreen(
                            navController = navController,
                            skillId = backStackEntry.arguments?.getString("skillId") ?: ""
                        )
                    }
                    composable("app_intelligence/{packageName}") { backStackEntry ->
                        AppIntelligenceScreen(
                            navController = navController,
                            packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                        )
                    }
                    composable("notifications") {
                        NotificationIntelligenceScreen(navController = navController)
                    }
                    composable("voice") {
                        VoiceModeScreen(navController = navController)
                    }
                    composable("screen_understanding") {
                        ScreenUnderstandingScreen(navController = navController)
                    }
                    composable("about") {
                        AboutScreen(navController = navController)
                    }
                    composable("privacy") {
                        PrivacyScreen(navController = navController)
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
