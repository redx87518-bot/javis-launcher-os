package com.javis.launcher.ui.screens.conversation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.data.db.entity.MessageEntity
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.theme.JavisTheme
import com.javis.launcher.voice.VoiceState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationScreen(
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

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
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JAVIS", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.primary)
                Text(
                    text = when (state.voiceState) {
                        VoiceState.LISTENING -> "Listening..."
                        VoiceState.THINKING -> "Thinking..."
                        VoiceState.SPEAKING -> "Speaking..."
                        else -> state.providerName
                    },
                    style = JavisTheme.typography.labelSmall,
                    color = JavisTheme.colors.onSurfaceDim
                )
            }
            // Provider indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (state.isOnline) JavisTheme.colors.success else JavisTheme.colors.warning)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("JAVIS", style = JavisTheme.typography.displayLarge,
                                color = JavisTheme.colors.primary.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("How can I help you today?",
                                style = JavisTheme.typography.bodyLarge,
                                color = JavisTheme.colors.onSurfaceDim)
                        }
                    }
                }
            } else {
                items(state.messages) { msg ->
                    MessageBubble(message = msg)
                }
            }

            // Typing indicator
            if (state.isProcessing) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Input area
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            cornerRadius = 24
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = state.inputText,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    textStyle = JavisTheme.typography.bodyMedium.copy(color = JavisTheme.colors.onBackground),
                    cursorBrush = SolidColor(JavisTheme.colors.primary),
                    maxLines = 4,
                    decorationBox = { inner ->
                        if (state.inputText.isEmpty()) {
                            Text("Message JAVIS...",
                                style = JavisTheme.typography.bodyMedium,
                                color = JavisTheme.colors.onSurfaceDim)
                        }
                        inner()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Voice button
                IconButton(
                    onClick = viewModel::toggleVoice,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.voiceState == VoiceState.LISTENING)
                                JavisTheme.colors.primary else JavisTheme.colors.glass
                        )
                ) {
                    Icon(
                        if (state.voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = if (state.voiceState == VoiceState.LISTENING)
                            JavisTheme.colors.background else JavisTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Send button
                if (state.inputText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = viewModel::sendMessage,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JavisTheme.colors.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send",
                            tint = JavisTheme.colors.background, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(JavisTheme.colors.primary.copy(alpha = 0.2f))
                    .border(1.dp, JavisTheme.colors.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("J", style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(
                        if (isUser) JavisTheme.colors.primary.copy(alpha = 0.2f)
                        else JavisTheme.colors.surfaceVariant
                    )
                    .border(
                        0.5.dp,
                        if (isUser) JavisTheme.colors.primary.copy(alpha = 0.4f)
                        else JavisTheme.colors.glassBorder,
                        RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = JavisTheme.typography.bodyMedium,
                    color = JavisTheme.colors.onBackground
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                style = JavisTheme.typography.labelSmall,
                color = JavisTheme.colors.onSurfaceDim
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(JavisTheme.colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("J", style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.primary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(JavisTheme.colors.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val delay = i * 200L
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(JavisTheme.colors.primary.copy(alpha = dotAlpha))
                )
            }
        }
    }
}
