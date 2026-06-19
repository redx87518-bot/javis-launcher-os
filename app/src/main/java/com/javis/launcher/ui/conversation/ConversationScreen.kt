package com.javis.launcher.ui.conversation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.ui.home.HomeViewModel
import com.javis.launcher.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConversationScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    val javisResponse by viewModel.javisResponse.collectAsState()
    val coreState by viewModel.coreState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val lastInput by viewModel.lastInput.collectAsState()
    val listState = rememberLazyListState()

    // Local conversation for display
    val messages = remember { mutableStateListOf<Pair<String, String>>() } // role, content

    LaunchedEffect(javisResponse) {
        if (javisResponse.isNotBlank() && javisResponse != "Thinking..." && javisResponse != "Processing...") {
            if (lastInput.isNotBlank() && (messages.isEmpty() || messages.last().second != lastInput)) {
                messages.add("user" to lastInput)
            }
            messages.add("assistant" to javisResponse)
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = JavisRed)
            }
            Text(
                "J·A·V·I·S  CHAT",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = JavisRed,
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = {
                messages.clear()
                viewModel.clearConversation()
            }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = JavisTextDim)
            }
        }

        HorizontalDivider(color = JavisGlassBorder)

        // Messages
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = JavisRed.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Start a conversation with JAVIS",
                        style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Try: \"Open WhatsApp\", \"Set alarm for 8pm\",\n\"Call mom\", \"Search for weather\"",
                        style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { (role, content) ->
                    MessageBubble(role = role, content = content)
                }
                if (coreState.name == "THINKING") {
                    item {
                        ThinkingIndicator()
                    }
                }
            }
        }

        HorizontalDivider(color = JavisGlassBorder)

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask JAVIS anything...", color = JavisTextDim)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JavisRed,
                    unfocusedBorderColor = JavisGlassBorder,
                    focusedTextColor = JavisTextPrimary,
                    unfocusedTextColor = JavisTextPrimary,
                    cursorColor = JavisRed
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            val msg = inputText.trim()
                            inputText = ""
                            keyboard?.hide()
                            viewModel.onTextInput(msg)
                        }
                    }
                )
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(listOf(JavisRed, JavisRedDark)),
                        CircleShape
                    )
                    .clickable {
                        if (inputText.isNotBlank()) {
                            val msg = inputText.trim()
                            inputText = ""
                            keyboard?.hide()
                            viewModel.onTextInput(msg)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSpeaking) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(role: String, content: String) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Brush.radialGradient(listOf(JavisRed.copy(0.3f), JavisRedDark.copy(0.1f))),
                        CircleShape
                    )
                    .border(1.dp, JavisRed.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("J", style = MaterialTheme.typography.labelMedium.copy(color = JavisRed))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isUser) "You" else "JAVIS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isUser) JavisTextDim else JavisRed,
                    letterSpacing = if (isUser) 0.sp else 2.sp
                )
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUser) JavisBgElevated else JavisBgCard,
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .border(
                        1.dp,
                        if (isUser) JavisGlassBorder else JavisRed.copy(0.3f),
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
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary)
                )
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(JavisBgElevated, CircleShape)
                    .border(1.dp, JavisGlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = JavisTextDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "d1")
    val alpha2 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, delayMillis = 170), RepeatMode.Reverse), label = "d2")
    val alpha3 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, delayMillis = 340), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Brush.radialGradient(listOf(JavisRed.copy(0.3f), JavisRedDark.copy(0.1f))),
                    CircleShape
                )
                .border(1.dp, JavisRed.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("J", style = MaterialTheme.typography.labelMedium.copy(color = JavisRed))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(JavisBgCard, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .border(1.dp, JavisRed.copy(0.3f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(JavisRed.copy(alpha1), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(JavisRed.copy(alpha2), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(JavisRed.copy(alpha3), CircleShape))
            }
        }
    }
}
