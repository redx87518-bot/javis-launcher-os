package com.javis.launcher.ui.mission

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.*
import com.javis.launcher.ui.theme.*

@Composable
fun MissionControlScreen(
    navController: NavController,
    viewModel: MissionViewModel = hiltViewModel()
) {
    val javisState by viewModel.javisState.collectAsState()
    val recentTasks by viewModel.recentTasks.collectAsState()
    val notifications by viewModel.unreadNotifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JavisBgCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Default.Dashboard, null, tint = JavisRed, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("MISSION CONTROL", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp))
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Status Grid
            item {
                Text("LIVE STATUS", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
                Spacer(Modifier.height(8.dp))
                StatusGrid(state = javisState)
            }

            // Current Task
            if (javisState.currentTask.isNotBlank()) {
                item {
                    ActiveTaskCard(task = javisState.currentTask, state = javisState.coreState)
                }
            }

            // Recent Tasks
            item {
                Text("TASK HISTORY", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
            }
            items(recentTasks) { task ->
                TaskCard(task = task)
            }
            if (recentTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(JavisBgCard, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tasks yet, Sir.", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim))
                    }
                }
            }

            // Notifications
            if (notifications.isNotEmpty()) {
                item {
                    Text("NOTIFICATIONS", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
                }
                items(notifications.take(10)) { notif ->
                    NotificationCard(notif = notif)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StatusGrid(state: JavisState) {
    val items = listOf(
        Triple("AI CORE", state.coreState.name, when(state.coreState) {
            CoreState.IDLE -> JavisTextDim
            CoreState.LISTENING -> JavisGreen
            CoreState.THINKING -> JavisBlue
            CoreState.SPEAKING -> JavisWhite
            CoreState.EXECUTING -> JavisGold
            else -> JavisRed
        }),
        Triple("PROVIDER", state.currentProvider.name, JavisGreen),
        Triple("NETWORK", if (state.isOnline) "ONLINE" else "OFFLINE", if (state.isOnline) JavisGreen else JavisRed),
        Triple("BATTERY", "${state.batteryLevel}%", when {
            state.batteryLevel > 50 -> JavisGreen
            state.batteryLevel > 20 -> JavisGold
            else -> JavisRed
        }),
        Triple("ALERTS", "${state.unreadNotifications} unread", if (state.unreadNotifications > 0) JavisGold else JavisGreenDim),
        Triple("STATUS", "OPERATIONAL", JavisGreen)
    )

    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value, color) ->
                    StatusCell(label = label, value = value, color = color, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatusCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cell")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .background(JavisBgCard, RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim, fontSize = 9.sp))
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(color = color.copy(alpha = pulse)),
            maxLines = 1
        )
    }
}

@Composable
fun ActiveTaskCard(task: String, state: CoreState) {
    val infiniteTransition = rememberInfiniteTransition(label = "task")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "border"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGold.copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = JavisGold,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text("EXECUTING", style = MaterialTheme.typography.labelSmall.copy(color = JavisGold, letterSpacing = 2.sp))
            Text(task, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
        }
    }
}

@Composable
fun TaskCard(task: com.javis.launcher.data.model.TaskEntity) {
    val statusColor = when (task.status) {
        "completed" -> JavisGreen
        "failed" -> JavisRed
        "running" -> JavisGold
        else -> JavisTextDim
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(10.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary), maxLines = 1)
            Text(
                formatTime(task.createdAt),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            task.status.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = statusColor)
        )
    }
}

@Composable
fun NotificationCard(notif: com.javis.launcher.data.model.NotificationCacheEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(10.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(JavisGlass, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(notif.appName.take(1).uppercase(), style = MaterialTheme.typography.bodyMedium.copy(color = JavisRed))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notif.appName, style = MaterialTheme.typography.bodySmall.copy(color = JavisRed))
            Text(notif.title, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary), maxLines = 1)
            Text(notif.text, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
