package com.javis.launcher.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.javis.launcher.ui.screens.appdrawer.AppDrawerScreen
import com.javis.launcher.ui.screens.conversation.ConversationScreen
import com.javis.launcher.ui.screens.dashboard.DashboardScreen
import com.javis.launcher.ui.screens.home.HomeScreen
import com.javis.launcher.ui.screens.settings.SettingsScreen
import com.javis.launcher.ui.screens.setup.SetupScreen

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home : Screen("home")
    object AppDrawer : Screen("app_drawer")
    object Conversation : Screen("conversation")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
}

@Composable
fun JavisNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }
        }
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(onSetupComplete = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Setup.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenDrawer = { navController.navigate(Screen.AppDrawer.route) },
                onOpenConversation = { navController.navigate(Screen.Conversation.route) },
                onOpenDashboard = { navController.navigate(Screen.Dashboard.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.AppDrawer.route) {
            AppDrawerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Conversation.route) {
            ConversationScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
