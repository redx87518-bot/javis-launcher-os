package com.javis.launcher.voice

import android.content.Context
import android.media.MediaPlayer
import com.google.gson.annotations.SerializedName
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

@Singleton
class ElevenLabsClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    suspend fun speak(text: String, prefs: VoicePrefs): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_monolingual_v1")
                put("voice_settings", JSONObject().apply {
                    put("stability", prefs.voiceStability)
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
            if (!response.isSuccessful) throw Exception("ElevenLabs error: ${response.code}")

            val audioFile = File(context.cacheDir, "javis_speech_${System.currentTimeMillis()}.mp3")
            response.body?.bytes()?.let { audioFile.writeBytes(it) }
                ?: throw Exception("Empty audio response")

            val player = MediaPlayer()
            player.setDataSource(audioFile.absolutePath)
            player.prepare()
            player.start()
            player.setOnCompletionListener {
                it.release()
                audioFile.delete()
            }
        }
    }
}
