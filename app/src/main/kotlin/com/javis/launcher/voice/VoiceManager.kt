package com.javis.launcher.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.javis.launcher.data.preferences.JavisPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING }

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: JavisPreferences,
    private val elevenLabsClient: ElevenLabsClient
) {
    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var onResult: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(0.95f)
            }
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        this.onResult = onResult
        _state.value = VoiceState.LISTENING
        _transcript.value = ""

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _state.value = VoiceState.THINKING
                }
                override fun onError(error: Int) {
                    _state.value = VoiceState.IDLE
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Please try again."
                        SpeechRecognizer.ERROR_NETWORK -> "No connection. Try again."
                        else -> ""
                    }
                    if (msg.isNotBlank()) speakTts(msg)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _transcript.value = text
                    _state.value = VoiceState.THINKING
                    if (text.isNotBlank()) onResult(text)
                    else _state.value = VoiceState.IDLE
                }
                override fun onPartialResults(partial: Bundle?) {
                    val matches = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    _transcript.value = matches?.firstOrNull() ?: ""
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = VoiceState.THINKING
    }

    suspend fun speak(text: String) {
        _state.value = VoiceState.SPEAKING
        val prefs = preferences.getVoicePrefs()

        if (prefs.elevenLabsKey.isNotBlank() && !prefs.useTts) {
            val result = elevenLabsClient.speak(text, prefs)
            if (result.isSuccess) {
                // ElevenLabs handles playback
            } else {
                speakTts(text)
            }
        } else {
            speakTts(text)
        }
    }

    fun speakTts(text: String) {
        _state.value = VoiceState.SPEAKING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "javis_${System.currentTimeMillis()}")
        // Reset to idle after a delay (crude estimate)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _state.value = VoiceState.IDLE
        }, (text.length * 60L).coerceAtLeast(1000L))
    }

    fun setIdle() { _state.value = VoiceState.IDLE }
    fun setThinking() { _state.value = VoiceState.THINKING }

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
