package com.javis.launcher.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.javis.launcher.data.preferences.JavisPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING }

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: JavisPreferences,
    val elevenLabsClient: ElevenLabsClient
) {
    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val _lastError = MutableStateFlow("")
    val lastError: StateFlow<String> = _lastError

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(0.90f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _state.value = VoiceState.IDLE
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = VoiceState.IDLE
                    }
                })
            }
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (_state.value == VoiceState.LISTENING) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _lastError.value = "Speech recognition not available on this device"
            return
        }
        _state.value = VoiceState.LISTENING
        _transcript.value = ""
        _lastError.value = ""

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceState.LISTENING
                }
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
                        SpeechRecognizer.ERROR_NETWORK -> "No connection for speech. Try again."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error. Please retry."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech system busy. Please wait."
                        else -> ""
                    }
                    if (msg.isNotBlank()) {
                        _lastError.value = msg
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _transcript.value = text
                    if (text.isNotBlank()) {
                        _state.value = VoiceState.THINKING
                        onResult(text)
                    } else {
                        _state.value = VoiceState.IDLE
                    }
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
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = VoiceState.THINKING
    }

    suspend fun speak(text: String) {
        if (text.isBlank()) {
            _state.value = VoiceState.IDLE
            return
        }
        _state.value = VoiceState.SPEAKING
        val prefs = preferences.getVoicePrefs()

        if (prefs.elevenLabsKey.isNotBlank() && prefs.voiceId.isNotBlank() && !prefs.useTts) {
            val result = elevenLabsClient.speak(text, prefs, onComplete = {
                _state.value = VoiceState.IDLE
            })
            if (result.isFailure) {
                speakTts(text)
            }
        } else {
            speakTts(text)
        }
    }

    fun speakTts(text: String) {
        _state.value = VoiceState.SPEAKING
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "javis_${System.currentTimeMillis()}")
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                _state.value = VoiceState.IDLE
            }, 800L)
        }
    }

    fun setIdle() { _state.value = VoiceState.IDLE }
    fun setThinking() { _state.value = VoiceState.THINKING }

    val elevenLabsStatus get() = elevenLabsClient.status

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
