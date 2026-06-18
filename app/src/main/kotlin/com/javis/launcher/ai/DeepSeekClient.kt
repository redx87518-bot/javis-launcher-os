package com.javis.launcher.ai

import com.javis.launcher.data.preferences.JavisPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

interface DeepSeekApi {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

@Singleton
class DeepSeekClient @Inject constructor(
    private val preferences: JavisPreferences,
    private val okHttpClient: OkHttpClient
) {
    private val api: DeepSeekApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }

    suspend fun chat(messages: List<AiMessage>, system: String): String = withContext(Dispatchers.IO) {
        val prefs = preferences.getAiProviderPrefs()
        if (prefs.deepSeekKey.isBlank()) throw IllegalStateException("DeepSeek key not set")

        val msgs = mutableListOf<Map<String, String>>()
        msgs.add(mapOf("role" to "system", "content" to system))
        messages.forEach { msgs.add(mapOf("role" to it.role, "content" to it.content)) }

        val response = api.chat(
            auth = "Bearer ${prefs.deepSeekKey}",
            request = OpenRouterRequest(
                model = prefs.deepSeekModel.ifBlank { "deepseek-chat" },
                messages = msgs
            )
        )
        response.choices.firstOrNull()?.message?.get("content")
            ?: throw Exception("Empty response from DeepSeek")
    }
}
