package com.javis.launcher.ui.screens.diagnostics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.ai.AiProvider
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.theme.JavisTheme

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisTheme.colors.background)
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = JavisTheme.colors.onSurface)
            }
            Column {
                Text("DIAGNOSTICS", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.primary)
                Text("Voice & AI system status", style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Voice diagnostics
            DiagSection(title = "VOICE SYSTEM", icon = Icons.Default.RecordVoiceOver) {
                StatusRow("Voice Provider", state.voiceProvider,
                    state.voiceProvider == "ElevenLabs")
                StatusRow("Voice ID", state.elevenLabsStatus.voiceId.ifBlank { "Not set" },
                    state.elevenLabsStatus.voiceId.isNotBlank())
                if (state.elevenLabsStatus.lastError.isNotBlank()) {
                    StatusRow("Last Error", state.elevenLabsStatus.lastError, false)
                }
                if (state.elevenLabsStatus.lastSuccessMs > 0) {
                    val ago = (System.currentTimeMillis() - state.elevenLabsStatus.lastSuccessMs) / 1000
                    StatusRow("Last Success", "${ago}s ago", true)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (state.elevenLabsTestResult.isNotBlank()) {
                    Text(
                        state.elevenLabsTestResult,
                        style = JavisTheme.typography.bodyMedium,
                        color = when {
                            state.elevenLabsTestResult.startsWith("✅") -> JavisTheme.colors.success
                            state.elevenLabsTestResult.startsWith("❌") -> JavisTheme.colors.error
                            else -> JavisTheme.colors.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagButton(
                        label = "Test Connection",
                        icon = Icons.Default.Wifi,
                        loading = state.elevenLabsTestLoading,
                        onClick = viewModel::testElevenLabs,
                        modifier = Modifier.weight(1f)
                    )
                    DiagButton(
                        label = "Test Speech",
                        icon = Icons.Default.VolumeUp,
                        loading = state.elevenLabsTestLoading,
                        onClick = viewModel::testVoiceSpeak,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // AI provider diagnostics
            DiagSection(title = "AI PROVIDERS", icon = Icons.Default.Psychology) {
                StatusRow("Active Provider", state.currentAiProvider, state.currentAiProvider != "Offline")
                if (state.lastAiResponseMs > 0) {
                    StatusRow("Last Response", "${state.lastAiResponseMs}ms",
                        state.lastAiResponseMs < 5000)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val providers = listOf(
                    AiProvider.GROQ to state.groqKey,
                    AiProvider.OPENROUTER to state.openRouterKey,
                    AiProvider.DEEPSEEK to state.deepSeekKey,
                    AiProvider.TOGETHER to state.togetherKey,
                    AiProvider.FIREWORKS to state.fireworksKey
                )

                providers.forEach { (provider, key) ->
                    val testResult = state.providerTests[provider]
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(provider.displayName, style = JavisTheme.typography.bodyMedium,
                                    color = JavisTheme.colors.onBackground)
                                Text(
                                    if (key.isBlank()) "Not configured" else "Key: ···${key.takeLast(4)}",
                                    style = JavisTheme.typography.labelSmall,
                                    color = JavisTheme.colors.onSurfaceDim
                                )
                                if (testResult != null && !testResult.isLoading) {
                                    Text(
                                        testResult.status.take(60),
                                        style = JavisTheme.typography.labelSmall,
                                        color = if (testResult.isSuccess == true) JavisTheme.colors.success
                                        else JavisTheme.colors.error
                                    )
                                }
                            }
                            if (key.isNotBlank()) {
                                if (testResult?.isLoading == true) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = JavisTheme.colors.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    TextButton(
                                        onClick = { viewModel.testProvider(provider) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Test", color = JavisTheme.colors.primary,
                                            style = JavisTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = JavisTheme.colors.divider.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DiagSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Icon(icon, null, tint = JavisTheme.colors.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = JavisTheme.typography.labelLarge, color = JavisTheme.colors.onSurfaceDim)
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOk) JavisTheme.colors.success else JavisTheme.colors.error)
            )
            Text(value, style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim)
        }
    }
}

@Composable
private fun DiagButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        border = BorderStroke(0.5.dp, JavisTheme.colors.primary.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisTheme.colors.primary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = JavisTheme.colors.primary, strokeWidth = 2.dp)
        } else {
            Icon(icon, null, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = JavisTheme.typography.labelSmall)
    }
}
