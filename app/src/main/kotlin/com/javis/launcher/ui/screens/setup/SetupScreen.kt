package com.javis.launcher.ui.screens.setup

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.ui.components.JavisOrb
import com.javis.launcher.ui.theme.JavisTheme
import com.javis.launcher.voice.VoiceState

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.setupComplete) {
        if (state.setupComplete) onSetupComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisTheme.colors.background)
            .systemBarsPadding()
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier.size(600.dp).align(Alignment.Center)
                .offset(y = (-100).dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(JavisTheme.colors.orbGlow.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Orb
            JavisOrb(
                voiceState = VoiceState.IDLE,
                onTap = {},
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn() + slideInVertically { it } togetherWith fadeOut() },
                label = "step"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        0 -> WelcomeStep()
                        1 -> NameStep(
                            name = state.name,
                            onNameChange = viewModel::onNameChange,
                            nickname = state.nickname,
                            onNicknameChange = viewModel::onNicknameChange
                        )
                        2 -> ProviderStep(
                            openRouterKey = state.openRouterKey,
                            onKeyChange = viewModel::onOpenRouterKeyChange,
                            groqKey = state.groqKey,
                            onGroqChange = viewModel::onGroqKeyChange
                        )
                        3 -> VoiceStep(
                            elevenLabsKey = state.elevenLabsKey,
                            onKeyChange = viewModel::onElevenLabsKeyChange
                        )
                        4 -> ReadyStep(name = state.name.ifBlank { "Sir" })
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Step indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == state.step) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i <= state.step) JavisTheme.colors.primary
                                else JavisTheme.colors.divider
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.step > 0) {
                    OutlinedButton(
                        onClick = viewModel::prevStep,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(0.5.dp, JavisTheme.colors.glassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisTheme.colors.onSurface)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (state.step == 4) viewModel.completeSetup()
                        else viewModel.nextStep()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = JavisTheme.colors.primary)
                ) {
                    Text(
                        if (state.step == 4) "Activate JAVIS" else "Continue",
                        color = JavisTheme.colors.background
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("JAVIS", style = JavisTheme.typography.displayLarge, color = JavisTheme.colors.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("LAUNCHER OS", style = JavisTheme.typography.labelLarge, color = JavisTheme.colors.onSurfaceDim)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your AI companion is ready to activate.\nLet's set up your experience.",
            style = JavisTheme.typography.bodyLarge,
            color = JavisTheme.colors.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NameStep(
    name: String, onNameChange: (String) -> Unit,
    nickname: String, onNicknameChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("What's your name?", style = JavisTheme.typography.headlineMedium, color = JavisTheme.colors.onBackground)
        Text("JAVIS will use this to address you.", style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurfaceDim)
        SetupField("Your name", name, onNameChange)
        SetupField("Nickname (optional)", nickname, onNicknameChange)
    }
}

@Composable
private fun ProviderStep(
    openRouterKey: String, onKeyChange: (String) -> Unit,
    groqKey: String, onGroqChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("AI Provider", style = JavisTheme.typography.headlineMedium, color = JavisTheme.colors.onBackground)
        Text(
            "Add at least one API key to enable full AI intelligence. You can skip and add later in Settings.",
            style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurfaceDim,
            textAlign = TextAlign.Center
        )
        SetupField("OpenRouter API Key", openRouterKey, onKeyChange, isPassword = true)
        SetupField("Groq API Key (optional)", groqKey, onGroqChange, isPassword = true)
    }
}

@Composable
private fun VoiceStep(elevenLabsKey: String, onKeyChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Voice Setup", style = JavisTheme.typography.headlineMedium, color = JavisTheme.colors.onBackground)
        Text(
            "Add your ElevenLabs API key for a human-like voice. JAVIS will use Android TTS if not provided.",
            style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurfaceDim,
            textAlign = TextAlign.Center
        )
        SetupField("ElevenLabs API Key (optional)", elevenLabsKey, onKeyChange, isPassword = true)
    }
}

@Composable
private fun ReadyStep(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ready, $name.", style = JavisTheme.typography.headlineLarge, color = JavisTheme.colors.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "JAVIS is configured and ready.\nTap activate to begin.",
            style = JavisTheme.typography.bodyLarge,
            color = JavisTheme.colors.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SetupField(
    label: String, value: String, onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JavisTheme.colors.surfaceVariant)
            .border(0.5.dp, JavisTheme.colors.glassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = JavisTheme.typography.bodyLarge.copy(color = JavisTheme.colors.onBackground),
            cursorBrush = SolidColor(JavisTheme.colors.primary),
            singleLine = true,
            visualTransformation = if (isPassword && value.isNotEmpty())
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(label, style = JavisTheme.typography.bodyLarge,
                    color = JavisTheme.colors.onSurfaceDim)
                inner()
            }
        )
    }
}
