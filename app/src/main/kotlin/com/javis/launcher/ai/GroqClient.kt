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

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

@Singleton
class GroqClient @Inject constructor(
    private val preferences: JavisPreferences,
    private val okHttpClient: OkHttpClient
) {
    private val api: GroqApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    suspend fun chat(messages: List<AiMessage>, system: String): String = withContext(Dispatchers.IO) {
        val prefs = preferences.getAiProviderPrefs()
        if (prefs.groqKey.isBlank()) throw IllegalStateException("Groq key not set")

        val msgs = mutableListOf<Map<String, String>>()
        msgs.add(mapOf("role" to "system", "content" to system))
        messages.forEach { msgs.add(mapOf("role" to it.role, "content" to it.content)) }

        val response = api.chat(
            auth = "Bearer ${prefs.groqKey}",
            request = OpenRouterRequest(
                model = prefs.groqModel.ifBlank { "llama3-70b-8192" },
                messages = msgs
            )
        )
        response.choices.firstOrNull()?.message?.get("content")
            ?: throw Exception("Empty response from Groq")
    }
}
