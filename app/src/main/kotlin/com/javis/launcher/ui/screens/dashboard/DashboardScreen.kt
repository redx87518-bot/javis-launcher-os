package com.javis.launcher.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.theme.JavisTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisTheme.colors.background)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = JavisTheme.colors.onSurface)
            }
            Text("MISSION CONTROL", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.primary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status cards row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusCard("Messages", state.unreadNotifs.toString(), Icons.Default.Message,
                        modifier = Modifier.weight(1f))
                    StatusCard("Reminders", state.upcomingReminders.size.toString(), Icons.Default.Alarm,
                        modifier = Modifier.weight(1f))
                    StatusCard("AI", state.providerName, Icons.Default.Psychology,
                        modifier = Modifier.weight(1f))
                }
            }

            // Upcoming Reminders
            if (state.upcomingReminders.isNotEmpty()) {
                item {
                    SectionTitle("UPCOMING REMINDERS")
                }
                items(state.upcomingReminders) { reminder ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(JavisTheme.colors.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reminder.title, style = JavisTheme.typography.bodyMedium,
                                    color = JavisTheme.colors.onBackground)
                                Text(
                                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                        .format(Date(reminder.triggerAt)),
                                    style = JavisTheme.typography.labelSmall,
                                    color = JavisTheme.colors.onSurfaceDim
                                )
                            }
                            IconButton(onClick = { viewModel.completeReminder(reminder.id) }) {
                                Icon(Icons.Default.CheckCircle, null, tint = JavisTheme.colors.success,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Recent Notifications
            item { SectionTitle("RECENT NOTIFICATIONS") }
            if (state.notifications.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No new notifications", style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurfaceDim)
                        }
                    }
                }
            } else {
                items(state.notifications) { notif ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(notif.appName, style = JavisTheme.typography.labelSmall,
                                        color = JavisTheme.colors.primary)
                                    Text(
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notif.timestamp)),
                                        style = JavisTheme.typography.labelSmall,
                                        color = JavisTheme.colors.onSurfaceDim
                                    )
                                }
                                Text(notif.title, style = JavisTheme.typography.bodyMedium,
                                    color = JavisTheme.colors.onBackground)
                                if (notif.text.isNotBlank()) {
                                    Text(notif.text, style = JavisTheme.typography.bodyMedium,
                                        color = JavisTheme.colors.onSurfaceDim, maxLines = 2)
                                }
                            }
                            if (!notif.isRead) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(JavisTheme.colors.primary))
                            }
                        }
                    }
                }
            }

            // AI Provider Status
            item { SectionTitle("AI SYSTEM STATUS") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Provider", style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurface)
                            Text(state.providerName, style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Response", style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurface)
                            Text(if (state.lastResponseMs > 0) "${state.lastResponseMs}ms" else "N/A",
                                style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurfaceDim)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status", style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurface)
                            Text(if (state.isOnline) "Online" else "Offline",
                                style = JavisTheme.typography.bodyMedium,
                                color = if (state.isOnline) JavisTheme.colors.success else JavisTheme.colors.warning)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = JavisTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = JavisTheme.typography.titleLarge, color = JavisTheme.colors.onBackground)
            Text(label, style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim)
}
