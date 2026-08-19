package com.javis.launcher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.javis.launcher.data.model.PermissionLevel
import com.javis.launcher.ui.theme.*

/**
 * Primary bottom navigation matching the spec main navigation:
 * Home, AI, Apps, Tasks, Settings.
 */
@Composable
fun JavisBottomNav(navController: NavController, currentRoute: String) {
    val items = listOf(
        Triple("home", "Home", Icons.Default.Home),
        Triple("conversation", "AI", Icons.Default.SmartToy),
        Triple("apps", "Apps", Icons.Default.Apps),
        Triple("mission", "Tasks", Icons.Default.Dashboard),
        Triple("settings", "Settings", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = JavisBgCard,
        contentColor = JavisTextPrimary
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = JavisRed,
                    selectedTextColor = JavisRed,
                    indicatorColor = JavisBgElevated,
                    unselectedIconColor = JavisTextDim,
                    unselectedTextColor = JavisTextDim
                )
            )
        }
    }
}

/**
 * Reusable confirmation dialog for sensitive actions (spec #22).
 */
@Composable
fun ConfirmationDialog(
    show: Boolean,
    title: String,
    message: String,
    level: PermissionLevel = PermissionLevel.COMMUNICATE,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    val color = when (level) {
        PermissionLevel.READ -> JavisGreen
        PermissionLevel.NAVIGATE -> JavisBlue
        PermissionLevel.INTERACT -> JavisGold
        PermissionLevel.COMMUNICATE -> JavisRed
        PermissionLevel.SENSITIVE -> Color(0xFFFF8C00)
        PermissionLevel.HIGH_IMPACT -> Color(0xFFFF4444)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JavisBgCard,
        titleContentColor = JavisTextPrimary,
        textContentColor = JavisTextSecondary,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = color, modifier = Modifier.size(20.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text(title)
            }
        },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = JavisTextDim) }
        }
    )
}
