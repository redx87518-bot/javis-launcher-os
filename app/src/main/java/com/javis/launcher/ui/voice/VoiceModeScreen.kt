package com.javis.launcher.ui.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.CoreState
import com.javis.launcher.ui.components.JavisBottomNav
import com.javis.launcher.ui.home.components.AiCore
import com.javis.launcher.ui.theme.*

@Composable
fun VoiceModeScreen(
    navController: NavController,
    viewModel: VoiceModeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val response by viewModel.response.collectAsState()

    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recognizer?.destroy()
            recognizer = null
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            viewModel.onError()
            return
        }
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { viewModel.onError() }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                viewModel.onFinalResult(matches?.firstOrNull() ?: "")
            }
            override fun onPartialResults(partial: Bundle?) {
                val matches = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotBlank()) viewModel.onPartialResult(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        viewModel.setListening()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        if (state == CoreState.LISTENING) viewModel.setIdle()
    }

    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                    modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text("VOICE MODE", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp, color = JavisRed))
            }

            Spacer(Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                AiCore(state = state, modifier = Modifier.size(220.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when (state) {
                    CoreState.LISTENING -> "Listening..."
                    CoreState.THINKING -> "Thinking..."
                    CoreState.SPEAKING -> "Speaking..."
                    CoreState.ERROR -> "Error"
                    else -> "Ready"
                },
                style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp)
            )

            Spacer(Modifier.height(12.dp))

            if (transcript.isNotBlank()) {
                Text(transcript, style = MaterialTheme.typography.bodyLarge.copy(color = JavisTextPrimary),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .background(JavisBgCard, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .border(1.dp, JavisGlassBorder, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(response, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary), textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))

            // Mic / Stop control
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (state == CoreState.LISTENING) JavisGreen else JavisRed,
                                if (state == CoreState.LISTENING) JavisGreen.copy(alpha = 0.3f) else JavisRedDark.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .clickable {
                        if (state == CoreState.LISTENING) stopListening()
                        else if (state == CoreState.SPEAKING) viewModel.stopSpeaking()
                        else startListening()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (state) {
                        CoreState.LISTENING -> Icons.Default.Stop
                        CoreState.SPEAKING -> Icons.Default.VolumeUp
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Mic", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Bottom nav overlay
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            JavisBottomNav(navController = navController, currentRoute = currentRoute)
        }
    }
}
