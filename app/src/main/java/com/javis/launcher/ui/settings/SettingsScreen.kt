package com.javis.launcher.ui.settings

import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.AiProvider
import com.javis.launcher.ui.theme.*

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val testResult by viewModel.testResult.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ArrowBack, "Back",
                tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("SETTINGS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp, color = JavisTextPrimary))
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI Providers Section
            item {
                SettingsSectionHeader(icon = Icons.Default.Psychology, title = "AI PROVIDERS")
            }

            items(AiProvider.values().filter { it != AiProvider.OFFLINE }) { provider ->
                AiProviderCard(
                    provider = provider,
                    viewModel = viewModel,
                    isTesting = isTesting && viewModel.testingProvider == provider,
                    testResult = if (viewModel.testingProvider == provider) testResult else null
                )
            }

            // ElevenLabs Voice
            item {
                SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "VOICE — ELEVENLABS")
            }
            item {
                ElevenLabsCard(viewModel = viewModel)
            }

            // Profile
            item {
                SettingsSectionHeader(icon = Icons.Default.Person, title = "PROFILE")
            }
            item {
                ProfileCard(viewModel = viewModel)
            }

            // Personality
            item {
                SettingsSectionHeader(icon = Icons.Default.AutoAwesome, title = "PERSONALITY")
            }
            item {
                PersonalityCard(viewModel = viewModel)
            }

            // Daily Briefing
            item {
                SettingsSectionHeader(icon = Icons.Default.Today, title = "DAILY BRIEFING")
            }
            item {
                DailyBriefingCard(viewModel = viewModel)
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = JavisRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
    }
}

@Composable
fun AiProviderCard(
    provider: AiProvider,
    viewModel: SettingsViewModel,
    isTesting: Boolean,
    testResult: String?
) {
    var expanded by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(viewModel.getApiKey(provider)) }
    var model by remember { mutableStateOf(viewModel.getModel(provider)) }
    var enabled by remember { mutableStateOf(viewModel.isProviderEnabled(provider)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, if (expanded) JavisRed.copy(0.5f) else JavisGlassBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(provider.name, style = MaterialTheme.typography.titleMedium.copy(color = JavisTextPrimary))
                Text(
                    getProviderDescription(provider),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (enabled && apiKey.isNotBlank()) JavisGreen else JavisTextDim,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = JavisTextDim, modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                Divider(color = JavisGlassBorder)
                Spacer(Modifier.height(12.dp))

                // Enabled toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            viewModel.setProviderEnabled(provider, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JavisGreen, checkedTrackColor = JavisGreenDim.copy(0.3f))
                    )
                }

                Spacer(Modifier.height(8.dp))

                // API Key
                JavisTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        viewModel.setApiKey(provider, it)
                    },
                    label = "API Key",
                    isPassword = true
                )

                Spacer(Modifier.height(8.dp))

                // Model
                JavisTextField(
                    value = model,
                    onValueChange = {
                        model = it
                        viewModel.setModel(provider, it)
                    },
                    label = "Model"
                )

                Spacer(Modifier.height(12.dp))

                // Test button
                Button(
                    onClick = { viewModel.testProvider(provider) },
                    enabled = !isTesting && apiKey.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = JavisRedDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(color = JavisWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Test Connection")
                    }
                }

                testResult?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (result.startsWith("✓")) JavisGreen else JavisRed
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ElevenLabsCard(viewModel: SettingsViewModel) {
    var apiKey by remember { mutableStateOf(viewModel.getElevenLabsKey()) }
    var voiceId by remember { mutableStateOf(viewModel.getVoiceId()) }
    var speed by remember { mutableStateOf(viewModel.getVoiceSpeed()) }
    var useElevenLabs by remember { mutableStateOf(viewModel.useElevenLabs()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Use ElevenLabs TTS", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary))
            Switch(
                checked = useElevenLabs,
                onCheckedChange = { useElevenLabs = it; viewModel.setUseElevenLabs(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = JavisGreen, checkedTrackColor = JavisGreenDim.copy(0.3f))
            )
        }
        Spacer(Modifier.height(12.dp))
        JavisTextField(value = apiKey, onValueChange = { apiKey = it; viewModel.setElevenLabsKey(it) }, label = "ElevenLabs API Key", isPassword = true)
        Spacer(Modifier.height(8.dp))
        JavisTextField(value = voiceId, onValueChange = { voiceId = it; viewModel.setVoiceId(it) }, label = "Voice ID (default: Adam)")
        Spacer(Modifier.height(12.dp))
        Text("Voice Speed: ${String.format("%.1f", speed)}x", style = MaterialTheme.typography.bodySmall.copy(color = JavisTextSecondary))
        Slider(
            value = speed, onValueChange = { speed = it; viewModel.setVoiceSpeed(it) },
            valueRange = 0.5f..2.0f, steps = 14,
            colors = SliderDefaults.colors(thumbColor = JavisRed, activeTrackColor = JavisRed)
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.testVoice() },
            colors = ButtonDefaults.buttonColors(containerColor = JavisRedDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Test Voice")
        }
    }
}

@Composable
fun ProfileCard(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf(viewModel.getUserName()) }
    var nickname by remember { mutableStateOf(viewModel.getNickname()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        JavisTextField(value = name, onValueChange = { name = it; viewModel.setUserName(it) }, label = "Your Name")
        JavisTextField(value = nickname, onValueChange = { nickname = it; viewModel.setNickname(it) }, label = "JAVIS calls you (e.g. Sir, Boss)")
    }
}

@Composable
fun PersonalityCard(viewModel: SettingsViewModel) {
    val modes = listOf("PROFESSIONAL", "FRIENDLY", "HUMOROUS", "JARVIS")
    var selected by remember { mutableStateOf(viewModel.getPersonalityMode()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Personality Mode", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            modes.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { selected = mode; viewModel.setPersonalityMode(mode) },
                    label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JavisRed,
                        selectedLabelColor = JavisWhite
                    )
                )
            }
        }
    }
}

@Composable
fun DailyBriefingCard(viewModel: SettingsViewModel) {
    var enabled by remember { mutableStateOf(viewModel.isDailyBriefingEnabled()) }
    val frequencies = listOf("EVERY_UNLOCK", "FIRST_UNLOCK", "EVERY_HOUR")
    var frequency by remember { mutableStateOf(viewModel.getBriefingFrequency()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Daily Briefing", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary))
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it; viewModel.setDailyBriefingEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = JavisGreen, checkedTrackColor = JavisGreenDim.copy(0.3f))
            )
        }
        if (enabled) {
            Spacer(Modifier.height(12.dp))
            Text("Frequency", style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frequencies.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq; viewModel.setBriefingFrequency(freq) },
                        label = { Text(freq.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = JavisRed, selectedLabelColor = JavisWhite)
                    )
                }
            }
        }
    }
}

@Composable
fun JavisTextField(value: String, onValueChange: (String) -> Unit, label: String, isPassword: Boolean = false) {
    var visible by remember { mutableStateOf(!isPassword) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !visible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = JavisTextDim,
                    modifier = Modifier.clickable { visible = !visible }.size(18.dp)
                )
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JavisRed,
            unfocusedBorderColor = JavisGlassBorder,
            focusedLabelColor = JavisRed,
            unfocusedLabelColor = JavisTextDim,
            focusedTextColor = JavisTextPrimary,
            unfocusedTextColor = JavisTextPrimary,
            cursorColor = JavisRed
        )
    )
}

private fun getProviderDescription(provider: AiProvider) = when (provider) {
    AiProvider.OPENROUTER -> "Access 100+ models including free tiers"
    AiProvider.GROQ -> "Ultra-fast inference, Llama 3 & Mixtral"
    AiProvider.DEEPSEEK -> "DeepSeek-V2 & DeepSeek-Coder"
    AiProvider.TOGETHER -> "Open-source models, fast & affordable"
    AiProvider.FIREWORKS -> "Blazing fast inference, many models"
    AiProvider.OFFLINE -> "Local offline inference"
}
