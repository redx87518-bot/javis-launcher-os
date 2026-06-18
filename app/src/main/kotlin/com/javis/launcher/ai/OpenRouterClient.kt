package com.javis.launcher.ai

import com.google.gson.annotations.SerializedName
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

data class OpenRouterRequest(
    val model: String,
    val messages: List<Map<String, String>>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Float = 0.7f
)

data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

data class OpenRouterChoice(
    val message: Map<String, String>
)

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/javis-launcher",
        @Header("X-Title") title: String = "JAVIS Launcher",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

@Singleton
class OpenRouterClient @Inject constructor(
    private val preferences: JavisPreferences,
    private val okHttpClient: OkHttpClient
) {
    private val api: OpenRouterApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }

    suspend fun chat(messages: List<AiMessage>, system: String): String = withContext(Dispatchers.IO) {
        val prefs = preferences.getAiProviderPrefs()
        if (prefs.openRouterKey.isBlank()) throw IllegalStateException("OpenRouter key not set")

        val msgs = mutableListOf<Map<String, String>>()
        msgs.add(mapOf("role" to "system", "content" to system))
        messages.forEach { msgs.add(mapOf("role" to it.role, "content" to it.content)) }

        val response = api.chat(
            auth = "Bearer ${prefs.openRouterKey}",
            request = OpenRouterRequest(
                model = prefs.openRouterModel.ifBlank { "mistralai/mistral-7b-instruct" },
                messages = msgs
            )
        )
        response.choices.firstOrNull()?.message?.get("content")
            ?: throw Exception("Empty response from OpenRouter")
    }
}
