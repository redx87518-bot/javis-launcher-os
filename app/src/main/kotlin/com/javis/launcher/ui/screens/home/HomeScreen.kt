package com.javis.launcher.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.ui.components.JavisOrb
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.components.NotificationBadge
import com.javis.launcher.ui.theme.JavisTheme
import com.javis.launcher.voice.VoiceState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenConversation: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisTheme.colors.background)
    ) {
        // Background ambient glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .offset(y = (-60).dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            JavisTheme.colors.orbGlow,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getCurrentDate(),
                        style = JavisTheme.typography.labelSmall,
                        color = JavisTheme.colors.onSurfaceDim
                    )
                    Text(
                        text = state.greeting,
                        style = JavisTheme.typography.titleMedium,
                        color = JavisTheme.colors.onBackground
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.unreadCount > 0) {
                        NotificationBadge(count = state.unreadCount, onClick = onOpenDashboard)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JavisTheme.colors.onSurfaceDim,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI ORB — the heart of JAVIS
            JavisOrb(
                voiceState = state.voiceState,
                onTap = viewModel::onOrbTapped,
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Voice transcript or last response
            AnimatedContent(
                targetState = when {
                    state.currentTranscript.isNotBlank() -> state.currentTranscript
                    state.taskStatus.isNotBlank() -> state.taskStatus
                    state.lastResponse.isNotBlank() -> state.lastResponse
                    else -> "Tap the orb to speak"
                },
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "status_text"
            ) { text ->
                Text(
                    text = text,
                    style = JavisTheme.typography.bodyMedium,
                    color = when {
                        state.taskStatus.isNotBlank() -> JavisTheme.colors.primary
                        state.lastResponse.isNotBlank() && state.currentTranscript.isBlank() ->
                            JavisTheme.colors.onSurface
                        else -> JavisTheme.colors.onSurfaceDim
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            QuickActionsRow(
                onOpenDrawer = onOpenDrawer,
                onOpenConversation = onOpenConversation,
                onOpenDashboard = onOpenDashboard
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Favorite Apps
            if (state.favoriteApps.isNotEmpty()) {
                SectionHeader(title = "FAVORITES")
                Spacer(modifier = Modifier.height(8.dp))
                AppGrid(apps = state.favoriteApps, onLaunch = { pkg ->
                    viewModel.processUserInput("open $pkg")
                })
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Recent Apps
            if (state.recentApps.isNotEmpty()) {
                SectionHeader(title = "RECENT")
                Spacer(modifier = Modifier.height(8.dp))
                AppGrid(apps = state.recentApps, onLaunch = { pkg ->
                    viewModel.processUserInput("open $pkg")
                })
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Upcoming reminders
            if (state.upcomingReminders.isNotEmpty()) {
                SectionHeader(title = "UPCOMING")
                Spacer(modifier = Modifier.height(8.dp))
                state.upcomingReminders.take(3).forEach { reminder ->
                    GlassCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                tint = JavisTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reminder.title,
                                style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatTime(reminder.triggerAt),
                                style = JavisTheme.typography.labelSmall,
                                color = JavisTheme.colors.onSurfaceDim
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Recent notifications
            if (state.recentNotifications.isNotEmpty()) {
                SectionHeader(title = "NOTIFICATIONS")
                Spacer(modifier = Modifier.height(8.dp))
                state.recentNotifications.take(3).forEach { notif ->
                    GlassCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = notif.appName,
                                    style = JavisTheme.typography.labelSmall,
                                    color = JavisTheme.colors.primary
                                )
                                Text(
                                    text = formatTimeAgo(notif.timestamp),
                                    style = JavisTheme.typography.labelSmall,
                                    color = JavisTheme.colors.onSurfaceDim
                                )
                            }
                            Text(
                                text = notif.title,
                                style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (notif.text.isNotBlank()) {
                                Text(
                                    text = notif.text,
                                    style = JavisTheme.typography.bodyMedium,
                                    color = JavisTheme.colors.onSurfaceDim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // App drawer hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDrawer() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(2.dp)
                            .background(JavisTheme.colors.divider, RoundedCornerShape(1.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "ALL APPS",
                        style = JavisTheme.typography.labelSmall,
                        color = JavisTheme.colors.onSurfaceDim
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuickActionsRow(
    onOpenDrawer: () -> Unit,
    onOpenConversation: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Apps,
            label = "Apps",
            onClick = onOpenDrawer,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Chat,
            label = "Chat",
            onClick = onOpenConversation,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Dashboard,
            label = "Mission",
            onClick = onOpenDashboard,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = JavisTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = JavisTheme.typography.labelSmall,
                color = JavisTheme.colors.onSurfaceDim
            )
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<com.javis.launcher.data.db.entity.InstalledAppEntity>,
    onLaunch: (String) -> Unit
) {
    val rows = apps.chunked(4)
    rows.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { app ->
                AppIcon(app = app, onClick = { onLaunch(app.packageName) }, modifier = Modifier.weight(1f))
            }
            repeat(4 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppIcon(
    app: com.javis.launcher.data.db.entity.InstalledAppEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(JavisTheme.colors.surfaceVariant)
                .border(
                    0.5.dp,
                    JavisTheme.colors.glassBorder,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(2).uppercase(),
                style = JavisTheme.typography.titleMedium,
                color = JavisTheme.colors.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.appName,
            style = JavisTheme.typography.labelSmall,
            color = JavisTheme.colors.onSurfaceDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = JavisTheme.typography.labelSmall,
        color = JavisTheme.colors.onSurfaceDim,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun getCurrentDate(): String {
    return SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
}

private fun formatTime(millis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}

private fun formatTimeAgo(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> "${diff / 86_400_000}d"
    }
}
