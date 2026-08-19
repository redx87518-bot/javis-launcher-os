package com.javis.launcher.ui.about

import android.content.SharedPreferences
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.local.ConversationDao
import com.javis.launcher.data.model.PermissionLevel
import com.javis.launcher.ui.components.ConfirmationDialog
import com.javis.launcher.ui.components.JavisBottomNav
import com.javis.launcher.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val prefs: SharedPreferences
) : ViewModel() {

    fun getFlag(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun setFlag(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun clearConversationHistory() {
        viewModelScope.launch { conversationDao.deleteOlderThan(System.currentTimeMillis() + 1_000_000_000L) }
    }
    fun resetAllData() { prefs.edit().clear().apply() }
}

@Composable
fun AboutScreen(navController: NavController, viewModel: AboutViewModel = hiltViewModel()) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""
    Column(
        modifier = Modifier.fillMaxSize().background(JavisBg).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(JavisBgCard).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text("ABOUT", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp, color = JavisRed))
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(JavisRed.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = JavisRed, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("JAVIS Launcher OS", style = MaterialTheme.typography.titleLarge.copy(color = JavisTextPrimary))
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim))
            Spacer(Modifier.height(16.dp))
            Text(
                "Your AI companion and mission control. JAVIS understands natural language and voice, " +
                "understands your apps and screen, and operates your phone with your permission.",
                style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            InfoRow("Module", "Android-native launcher")
            InfoRow("AI", "Pluggable multi-provider router")
            InfoRow("Voice", "On-device STT + TTS fallback")
            InfoRow("Automation", "Accessibility + Android intents")
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navController.navigate("privacy") }, colors = ButtonDefaults.buttonColors(containerColor = JavisRed)) {
                Icon(Icons.Default.PrivacyTip, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Privacy & Security")
            }
        }
    }
    JavisBottomNav(navController = navController, currentRoute = currentRoute)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(JavisBgCard, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim, letterSpacing = 1.sp), modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
    }
}

@Composable
fun PrivacyScreen(navController: NavController, viewModel: AboutViewModel = hiltViewModel()) {
    var showConfirmClear by remember { mutableStateOf(false) }
    var showConfirmReset by remember { mutableStateOf(false) }

    var allowCloud by remember { mutableStateOf(viewModel.getFlag("allow_cloud_ai", true)) }
    var saveHistory by remember { mutableStateOf(viewModel.getFlag("save_history", true)) }
    var readAloud by remember { mutableStateOf(viewModel.getFlag("read_notifications_aloud", false)) }

    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""

    Column(
        modifier = Modifier.fillMaxSize().background(JavisBg).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(JavisBgCard).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text("PRIVACY & SECURITY", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp, color = JavisRed))
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Your data stays local by default. Cloud AI is used only when you configure a provider and only for the request you make.", style = MaterialTheme.typography.bodySmall.copy(color = JavisTextSecondary))
            }
            item { PrefToggle("Allow cloud AI providers", allowCloud) { allowCloud = it; viewModel.setFlag("allow_cloud_ai", it) } }
            item { PrefToggle("Save conversation history", saveHistory) { saveHistory = it; viewModel.setFlag("save_history", it) } }
            item { PrefToggle("Read notifications aloud", readAloud) { readAloud = it; viewModel.setFlag("read_notifications_aloud", it) } }
            item {
                Button(onClick = { showConfirmClear = true }, colors = ButtonDefaults.buttonColors(containerColor = JavisBgElevated),
                    border = ButtonDefaults.outlinedButtonBorder, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear conversation history", color = JavisGold)
                }
            }
            item {
                Button(onClick = { showConfirmReset = true }, colors = ButtonDefaults.buttonColors(containerColor = JavisRed.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, JavisRed.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                    Text("Reset all local data", color = JavisRed)
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }

    ConfirmationDialog(
        show = showConfirmClear,
        title = "Clear history?",
        message = "This permanently deletes your local conversation history. This cannot be undone.",
        level = PermissionLevel.SENSITIVE,
        onConfirm = { viewModel.clearConversationHistory(); showConfirmClear = false },
        onDismiss = { showConfirmClear = false }
    )
    ConfirmationDialog(
        show = showConfirmReset,
        title = "Reset all data?",
        message = "This erases all local settings, saved keys and preferences on this device.",
        level = PermissionLevel.HIGH_IMPACT,
        onConfirm = { viewModel.resetAllData(); showConfirmReset = false },
        onDismiss = { showConfirmReset = false }
    )

    JavisBottomNav(navController = navController, currentRoute = currentRoute)
}

@Composable
private fun PrefToggle(label: String, value: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(JavisBgCard, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary), modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = JavisRed, checkedTrackColor = JavisRed.copy(alpha = 0.4f)))
    }
}
