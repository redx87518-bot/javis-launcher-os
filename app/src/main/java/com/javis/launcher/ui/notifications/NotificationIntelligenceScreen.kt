package com.javis.launcher.ui.notifications

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.NotificationCacheEntity
import com.javis.launcher.ui.components.JavisBottomNav
import com.javis.launcher.ui.theme.*

@Composable
fun NotificationIntelligenceScreen(
    navController: NavController,
    viewModel: NotificationIntelligenceViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()

    val grouped = remember(notifications) {
        notifications.groupBy { it.appName.ifBlank { it.packageName } }
            .toSortedMap()
    }
    val important = remember(notifications) { notifications.filter { isImportant(it) } }

    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
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
            Text("NOTIFICATIONS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp, color = JavisRed))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewModel.markAllRead() }) {
                Text("Mark all read", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No unread notifications, Sir.", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim), textAlign = TextAlign.Center)
                    }
                }
            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${notifications.size} unread", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
                        Spacer(Modifier.width(8.dp))
                        if (important.isNotEmpty()) {
                            Text("${important.size} important", style = MaterialTheme.typography.labelSmall.copy(color = JavisRed, letterSpacing = 1.sp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.summarize(grouped) }, colors = ButtonDefaults.buttonColors(containerColor = JavisRed), modifier = Modifier.fillMaxWidth()) {
                        if (isSummarizing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Summarize, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI Summary")
                    }
                }

                if (summary.isNotBlank()) {
                    item {
                        Box(Modifier.fillMaxWidth().background(JavisBgCard, RoundedCornerShape(12.dp)).border(1.dp, JavisRed.copy(0.3f), RoundedCornerShape(12.dp)).padding(14.dp)) {
                            Text(summary, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
                        }
                    }
                }

                if (important.isNotEmpty()) {
                    item { Text("IMPORTANT", style = MaterialTheme.typography.labelSmall.copy(color = JavisRed, letterSpacing = 2.sp)) }
                    items(important, key = { it.id }) { notif ->
                        NotificationRow(notif, true) { viewModel.markRead(it.id) }
                    }
                }

                item { Text("ALL", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp)) }
                items(notifications, key = { it.id }) { notif ->
                    NotificationRow(notif, isImportant(notif)) { viewModel.markRead(it.id) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    JavisBottomNav(navController = navController, currentRoute = currentRoute)
}

@Composable
private fun NotificationRow(notif: NotificationCacheEntity, important: Boolean, onRead: (NotificationCacheEntity) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(10.dp))
            .border(1.dp, if (important) JavisRed.copy(0.4f) else JavisGlassBorder, RoundedCornerShape(10.dp))
            .clickable { onRead(notif) }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(if (important) JavisRed.copy(0.15f) else JavisGlass, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (important) Icons.Default.PriorityHigh else Icons.Default.Notifications, null, tint = if (important) JavisRed else JavisTextDim, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notif.appName, style = MaterialTheme.typography.bodySmall.copy(color = JavisRed))
            if (notif.title.isNotBlank()) Text(notif.title, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (notif.text.isNotBlank()) Text(notif.text, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

private val IMPORTANT_KEYWORDS = listOf(
    "work", "github", "bank", "urgent", "meeting", "missed", "security", "alert",
    "payment", "delivery", "code", "otp", "verify", "important", "calendar"
)

private fun isImportant(notif: NotificationCacheEntity): Boolean {
    val text = "${notif.title} ${notif.text} ${notif.appName}".lowercase()
    return IMPORTANT_KEYWORDS.any { text.contains(it) }
}
