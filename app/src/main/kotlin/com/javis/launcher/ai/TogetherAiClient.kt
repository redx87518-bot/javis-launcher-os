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

interface TogetherApi {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

@Singleton
class TogetherAiClient @Inject constructor(
    private val preferences: JavisPreferences,
    private val okHttpClient: OkHttpClient
) {
    private val api: TogetherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.together.xyz/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TogetherApi::class.java)
    }

    suspend fun chat(messages: List<AiMessage>, system: String): String = withContext(Dispatchers.IO) {
        val prefs = preferences.getAiProviderPrefs()
        if (prefs.togetherKey.isBlank()) throw IllegalStateException("Together AI key not set")

        val msgs = mutableListOf<Map<String, String>>()
        msgs.add(mapOf("role" to "system", "content" to system))
        messages.forEach { msgs.add(mapOf("role" to it.role, "content" to it.content)) }

        val response = api.chat(
            auth = "Bearer ${prefs.togetherKey}",
            request = OpenRouterRequest(
                model = prefs.togetherModel.ifBlank { "meta-llama/Llama-3-70b-chat-hf" },
                messages = msgs
            )
        )
        response.choices.firstOrNull()?.message?.get("content")
            ?: throw Exception("Empty response from Together AI")
    }
}
