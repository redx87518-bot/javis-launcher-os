package com.javis.launcher.voice

import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import com.javis.launcher.data.network.ElevenLabsApiService
import com.javis.launcher.data.model.TtsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val elevenLabsService: ElevenLabsApiService,
    private val prefs: SharedPreferences
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.US)
            tts?.setSpeechRate(prefs.getFloat("tts_speed", 0.9f))
            ttsReady = true
        }
    }

    suspend fun speak(text: String) {
        _isSpeaking.value = true
        val elevenLabsKey = prefs.getString("elevenlabs_api_key", "") ?: ""
        val useElevenLabs = prefs.getBoolean("use_elevenlabs", true) && elevenLabsKey.isNotBlank()

        if (useElevenLabs) {
            val success = speakWithElevenLabs(text, elevenLabsKey)
            if (!success) speakWithTts(text)
        } else {
            speakWithTts(text)
        }
        _isSpeaking.value = false
    }

    private suspend fun speakWithElevenLabs(text: String, apiKey: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val voiceId = prefs.getString("elevenlabs_voice_id", "pNInz6obpgDQGcFmaJgB") ?: "pNInz6obpgDQGcFmaJgB"
                val speed = prefs.getFloat("voice_speed", 1.0f)
                val request = TtsRequest(
                    text = text,
                    model_id = "eleven_monolingual_v1"
                )
                val response = elevenLabsService.textToSpeech(
                    voiceId = voiceId,
                    apiKey = apiKey,
                    request = request
                )
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes() ?: return@withContext false
                    val audioFile = File(context.cacheDir, "javis_tts.mp3")
                    audioFile.writeBytes(audioBytes)
                    playAudioFile(audioFile)
                    true
                } else false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun playAudioFile(file: File) {
        try {
            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speakWithTts(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "javis_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun updateSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
        prefs.edit().putFloat("tts_speed", speed).apply()
    }

    suspend fun getVoices(): List<com.javis.launcher.data.model.VoiceModel> {
        val apiKey = prefs.getString("elevenlabs_api_key", "") ?: ""
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = withContext(Dispatchers.IO) {
                elevenLabsService.getVoices(apiKey)
            }
            response.body()?.voices ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun destroy() {
        tts?.shutdown()
    }
}
