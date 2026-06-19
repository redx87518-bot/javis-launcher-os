package com.javis.launcher.voice

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.javis.launcher.data.network.ElevenLabsApiService
import com.javis.launcher.data.model.TtsRequest
import com.javis.launcher.data.model.VoiceSettings
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val elevenLabsService: ElevenLabsApiService,
    private val prefs: SharedPreferences
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _elevenLabsStatus = MutableStateFlow(ElevenLabsStatus.UNKNOWN)
    val elevenLabsStatus: StateFlow<ElevenLabsStatus> = _elevenLabsStatus.asStateFlow()

    enum class ElevenLabsStatus { UNKNOWN, CONNECTED, FAILED, NO_KEY }

    // Default voice ID — user's voice from ElevenLabs
    companion object {
        const val DEFAULT_VOICE_ID = "9375G6zswFk7v9bKTVQF"
        const val DEFAULT_MODEL = "eleven_multilingual_v2"
    }

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(prefs.getFloat("tts_speed", 0.9f))
            tts?.setPitch(prefs.getFloat("tts_pitch", 1.0f))
            ttsReady = true
        }
    }

    suspend fun speak(text: String) {
        if (text.isBlank()) return
        _isSpeaking.value = true

        // Clean text — remove any JSON leftovers or special chars
        val cleanText = cleanForSpeech(text)
        if (cleanText.isBlank()) {
            _isSpeaking.value = false
            return
        }

        val elevenLabsKey = getElevenLabsKey()
        val useElevenLabs = prefs.getBoolean("use_elevenlabs", true) && elevenLabsKey.isNotBlank()

        if (useElevenLabs) {
            val success = speakWithElevenLabs(cleanText, elevenLabsKey)
            if (!success) {
                _elevenLabsStatus.value = ElevenLabsStatus.FAILED
                speakWithTts(cleanText)
            } else {
                _elevenLabsStatus.value = ElevenLabsStatus.CONNECTED
            }
        } else {
            speakWithTts(cleanText)
        }

        _isSpeaking.value = false
    }

    private fun getElevenLabsKey(): String {
        // Check prefs first, then return empty
        return prefs.getString("elevenlabs_api_key", "") ?: ""
    }

    private fun getVoiceId(): String {
        return prefs.getString("elevenlabs_voice_id", DEFAULT_VOICE_ID) ?: DEFAULT_VOICE_ID
    }

    private suspend fun speakWithElevenLabs(text: String, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val voiceId = getVoiceId()
                val model = prefs.getString("elevenlabs_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
                val stability = prefs.getFloat("el_stability", 0.5f)
                val similarity = prefs.getFloat("el_similarity", 0.8f)

                val request = TtsRequest(
                    text = text,
                    model_id = model,
                    voice_settings = VoiceSettings(
                        stability = stability,
                        similarity_boost = similarity,
                        style = 0.2f,
                        use_speaker_boost = true
                    )
                )

                val response = elevenLabsService.textToSpeech(
                    voiceId = voiceId,
                    apiKey = apiKey,
                    request = request
                )

                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        val audioFile = File(context.cacheDir, "javis_speech_${System.currentTimeMillis()}.mp3")
                        audioFile.writeBytes(audioBytes)
                        withContext(Dispatchers.Main) {
                            playAudioFile(audioFile)
                        }
                        true
                    } else false
                } else {
                    android.util.Log.e("VoiceManager", "ElevenLabs error: ${response.code()} ${response.message()}")
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("VoiceManager", "ElevenLabs exception: ${e.message}")
                false
            }
        }
    }

    private fun playAudioFile(file: File) {
        try {
            // Release previous player
            mediaPlayer?.release()
            mediaPlayer = null

            // Request audio focus
            requestAudioFocus()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { mp ->
                    mp.release()
                    if (mediaPlayer == mp) mediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    file.delete()
                    abandonAudioFocus()
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceManager", "Audio playback error: ${e.message}")
            file.delete()
        }
    }

    private suspend fun speakWithTts(text: String) = suspendCoroutine<Unit> { cont ->
        if (!ttsReady || tts == null) {
            cont.resume(Unit)
            return@suspendCoroutine
        }
        val utteranceId = "javis_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { cont.resume(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { cont.resume(Unit) }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build()
            )
        }
    }

    private fun cleanForSpeech(text: String): String {
        // Remove JSON blocks
        var result = text
        var depth = 0
        val sb = StringBuilder()
        for (ch in text) {
            when {
                ch == '{' -> depth++
                ch == '}' -> depth--
                depth == 0 -> sb.append(ch)
            }
        }
        result = sb.toString()
        // Remove markdown
        result = result.replace(Regex("\\*+"), "")
            .replace(Regex("#+\\s"), "")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .replace(Regex("```[\\s\\S]*?```"), "")
            .trim()
        return result
    }

    fun stopSpeaking() {
        tts?.stop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isSpeaking.value = false
    }

    fun updateSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
        prefs.edit().putFloat("tts_speed", speed).apply()
    }

    suspend fun testElevenLabs(): Boolean {
        val key = getElevenLabsKey()
        if (key.isBlank()) {
            _elevenLabsStatus.value = ElevenLabsStatus.NO_KEY
            return false
        }
        return speakWithElevenLabs("JAVIS voice online.", key)
    }

    suspend fun getAvailableVoices(): List<com.javis.launcher.data.model.VoiceModel> {
        val apiKey = getElevenLabsKey()
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
