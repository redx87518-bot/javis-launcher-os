package com.javis.launcher.voice

import android.content.Context
import android.media.MediaPlayer
import com.javis.launcher.data.preferences.VoicePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ElevenLabsStatus(
    val isConfigured: Boolean = false,
    val lastError: String = "",
    val lastSuccessMs: Long = 0L,
    val currentProvider: String = "Android TTS",
    val voiceId: String = "",
    val lastRequest: String = ""
)

@Singleton
class ElevenLabsClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private var _status = ElevenLabsStatus()
    val status: ElevenLabsStatus get() = _status

    suspend fun speak(
        text: String,
        prefs: VoicePrefs,
        onComplete: () -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            _status = _status.copy(
                isConfigured = prefs.elevenLabsKey.isNotBlank(),
                voiceId = prefs.voiceId,
                lastRequest = text.take(80)
            )

            val body = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_monolingual_v1")
                put("voice_settings", JSONObject().apply {
                    put("stability", prefs.voiceStability.toDouble())
                    put("similarity_boost", 0.75)
                    put("style", 0.0)
                    put("use_speaker_boost", true)
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/${prefs.voiceId}")
                .addHeader("xi-api-key", prefs.elevenLabsKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "(no body)"
                val err = "ElevenLabs HTTP ${response.code}: $errBody"
                _status = _status.copy(lastError = err, currentProvider = "Android TTS")
                throw Exception(err)
            }

            val audioBytes = response.body?.bytes()
                ?: throw Exception("Empty audio response from ElevenLabs")

            val audioFile = File(context.cacheDir, "javis_tts_${System.currentTimeMillis()}.mp3")
            audioFile.writeBytes(audioBytes)

            withContext(Dispatchers.Main) {
                val player = MediaPlayer()
                try {
                    player.setDataSource(audioFile.absolutePath)
                    player.prepare()
                    player.start()
                    _status = _status.copy(
                        lastSuccessMs = System.currentTimeMillis(),
                        lastError = "",
                        currentProvider = "ElevenLabs"
                    )
                    player.setOnCompletionListener { mp ->
                        mp.release()
                        audioFile.delete()
                        onComplete()
                    }
                    player.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        audioFile.delete()
                        _status = _status.copy(lastError = "MediaPlayer error", currentProvider = "Android TTS")
                        onComplete()
                        true
                    }
                } catch (e: Exception) {
                    player.release()
                    audioFile.delete()
                    _status = _status.copy(lastError = e.message ?: "Playback error", currentProvider = "Android TTS")
                    onComplete()
                    throw e
                }
            }
        }.onFailure { e ->
            _status = _status.copy(lastError = e.message ?: "Unknown error", currentProvider = "Android TTS")
        }
    }

    suspend fun testConnection(apiKey: String, voiceId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/voices/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .get()
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: "{}"
                val name = try {
                    JSONObject(bodyStr).optString("name", "Unknown Voice")
                } catch (_: Exception) { "Unknown" }
                _status = _status.copy(isConfigured = true, lastError = "", voiceId = voiceId, currentProvider = "ElevenLabs")
                "✅ Connected — Voice: $name"
            } else {
                val errBody = response.body?.string() ?: ""
                val err = "HTTP ${response.code}: $errBody"
                _status = _status.copy(lastError = err)
                throw Exception(err)
            }
        }
    }

    suspend fun testSpeak(apiKey: String, voiceId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val testPrefs = VoicePrefs(
                elevenLabsKey = apiKey,
                voiceId = voiceId,
                voiceSpeed = 1.0f,
                voiceStability = 0.5f
            )
            speak("Hello. JAVIS voice system is online and operational.", testPrefs).getOrThrow()
            "✅ Voice test successful"
        }
    }
}
