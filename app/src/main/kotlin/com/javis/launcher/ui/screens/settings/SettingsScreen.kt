package com.javis.launcher.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.theme.JavisTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisTheme.colors.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = JavisTheme.colors.onSurface)
            }
            Text("SETTINGS", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.primary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile section
            SettingsSection(title = "PROFILE", icon = Icons.Default.Person) {
                SettingsTextField("Your Name", state.name, viewModel::onNameChange)
                SettingsTextField("Nickname", state.nickname, viewModel::onNicknameChange)
                SettingsButton("Save Profile", onClick = viewModel::saveProfile)
            }

            // AI Providers
            SettingsSection(title = "AI PROVIDERS", icon = Icons.Default.Psychology) {
                ProviderSection(
                    name = "OpenRouter",
                    apiKey = state.openRouterKey,
                    model = state.openRouterModel,
                    enabled = state.openRouterEnabled,
                    onKeyChange = viewModel::onOpenRouterKeyChange,
                    onModelChange = viewModel::onOpenRouterModelChange,
                    onToggle = viewModel::toggleOpenRouter,
                    modelHint = "e.g. mistralai/mistral-7b-instruct"
                )
                HorizontalDivider(color = JavisTheme.colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                ProviderSection(
                    name = "Groq",
                    apiKey = state.groqKey,
                    model = state.groqModel,
                    enabled = state.groqEnabled,
                    onKeyChange = viewModel::onGroqKeyChange,
                    onModelChange = viewModel::onGroqModelChange,
                    onToggle = viewModel::toggleGroq,
                    modelHint = "e.g. llama3-70b-8192"
                )
                HorizontalDivider(color = JavisTheme.colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                ProviderSection(
                    name = "DeepSeek",
                    apiKey = state.deepSeekKey,
                    model = state.deepSeekModel,
                    enabled = state.deepSeekEnabled,
                    onKeyChange = viewModel::onDeepSeekKeyChange,
                    onModelChange = viewModel::onDeepSeekModelChange,
                    onToggle = viewModel::toggleDeepSeek,
                    modelHint = "e.g. deepseek-chat"
                )
                HorizontalDivider(color = JavisTheme.colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                ProviderSection(
                    name = "Together AI",
                    apiKey = state.togetherKey,
                    model = state.togetherModel,
                    enabled = state.togetherEnabled,
                    onKeyChange = viewModel::onTogetherKeyChange,
                    onModelChange = viewModel::onTogetherModelChange,
                    onToggle = viewModel::toggleTogether,
                    modelHint = "e.g. meta-llama/Llama-3-70b-chat-hf"
                )
                HorizontalDivider(color = JavisTheme.colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                ProviderSection(
                    name = "Fireworks AI",
                    apiKey = state.fireworksKey,
                    model = state.fireworksModel,
                    enabled = state.fireworksEnabled,
                    onKeyChange = viewModel::onFireworksKeyChange,
                    onModelChange = viewModel::onFireworksModelChange,
                    onToggle = viewModel::toggleFireworks,
                    modelHint = "e.g. accounts/fireworks/models/llama-v3-70b-instruct"
                )
                SettingsButton("Save AI Settings", onClick = viewModel::saveAiSettings)
            }

            // Voice
            SettingsSection(title = "VOICE", icon = Icons.Default.RecordVoiceOver) {
                SettingsTextField("ElevenLabs API Key", state.elevenLabsKey, viewModel::onElevenLabsKeyChange, isPassword = true)
                SettingsTextField("Voice ID", state.voiceId, viewModel::onVoiceIdChange)
                Column {
                    Text("Voice Speed: ${state.voiceSpeed}x", style = JavisTheme.typography.bodyMedium,
                        color = JavisTheme.colors.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                    Slider(
                        value = state.voiceSpeed,
                        onValueChange = viewModel::onVoiceSpeedChange,
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = JavisTheme.colors.primary,
                            activeTrackColor = JavisTheme.colors.primary)
                    )
                }
                SettingsToggle("Use Android TTS (Fallback)", state.useTts, viewModel::toggleTts)
                SettingsButton("Save Voice Settings", onClick = viewModel::saveVoiceSettings)
            }

            // Greeting
            SettingsSection(title = "GREETING", icon = Icons.Default.WavingHand) {
                SettingsToggle("Greeting on Unlock", state.greetingEnabled, viewModel::toggleGreeting)
                SettingsToggle("Voice Greeting", state.voiceGreetingEnabled, viewModel::toggleVoiceGreeting)
                SettingsToggle("Daily Briefing", state.briefingEnabled, viewModel::toggleBriefing)
                SettingsButton("Save Greeting Settings", onClick = viewModel::saveGreetingSettings)
            }

            // Memory
            SettingsSection(title = "MEMORY", icon = Icons.Default.Memory) {
                Text("JAVIS has stored memories from your conversations.",
                    style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp))
                SettingsButton("Clear All Memories", onClick = viewModel::clearMemories,
                    isDestructive = true)
            }

            // Permissions
            SettingsSection(title = "PERMISSIONS", icon = Icons.Default.Security) {
                PermissionRow("Notification Access", Icons.Default.Notifications, state.hasNotificationAccess)
                PermissionRow("Accessibility Service", Icons.Default.Accessibility, state.hasAccessibility)
                PermissionRow("Contacts", Icons.Default.Contacts, state.hasContacts)
                PermissionRow("Microphone", Icons.Default.Mic, state.hasMicrophone)
                PermissionRow("Phone Calls", Icons.Default.Phone, state.hasPhone)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(icon, null, tint = JavisTheme.colors.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = JavisTheme.typography.labelLarge, color = JavisTheme.colors.onSurfaceDim)
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ProviderSection(
    name: String,
    apiKey: String,
    model: String,
    enabled: Boolean,
    onKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit,
    modelHint: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.onBackground)
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = JavisTheme.colors.background,
                    checkedTrackColor = JavisTheme.colors.primary)
            )
        }
        SettingsTextField("API Key", apiKey, onKeyChange, isPassword = true)
        SettingsTextField("Model", model, onModelChange, hint = modelHint)
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    hint: String = ""
) {
    Column {
        Text(label, style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim,
            modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(JavisTheme.colors.surfaceVariant)
                .border(0.5.dp, JavisTheme.colors.glassBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = JavisTheme.typography.bodyMedium.copy(color = JavisTheme.colors.onBackground),
                cursorBrush = SolidColor(JavisTheme.colors.primary),
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    val displayHint = if (hint.isNotBlank()) hint else "Enter $label"
                    if (value.isEmpty()) {
                        Text(displayHint, style = JavisTheme.typography.bodyMedium,
                            color = JavisTheme.colors.onSurfaceDim)
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onBackground)
        Switch(
            checked = value, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = JavisTheme.colors.background,
                checkedTrackColor = JavisTheme.colors.primary)
        )
    }
}

@Composable
private fun SettingsButton(label: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDestructive) JavisTheme.colors.error.copy(alpha = 0.15f)
            else JavisTheme.colors.primary.copy(alpha = 0.15f)
        ),
        border = BorderStroke(0.5.dp, if (isDestructive) JavisTheme.colors.error.copy(alpha = 0.5f)
        else JavisTheme.colors.primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, color = if (isDestructive) JavisTheme.colors.error else JavisTheme.colors.primary)
    }
}

@Composable
private fun PermissionRow(label: String, icon: ImageVector, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = JavisTheme.colors.onSurfaceDim, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onBackground)
        }
        Text(
            if (granted) "Granted" else "Required",
            style = JavisTheme.typography.labelSmall,
            color = if (granted) JavisTheme.colors.success else JavisTheme.colors.warning
        )
    }
}
