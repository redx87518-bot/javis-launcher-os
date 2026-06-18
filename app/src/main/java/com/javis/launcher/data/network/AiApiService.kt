package com.javis.launcher.data.network

import com.javis.launcher.data.model.ChatRequest
import com.javis.launcher.data.model.ChatResponse
import com.javis.launcher.data.model.TtsRequest
import com.javis.launcher.data.model.VoicesResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * OpenAI-compatible chat completions endpoint.
 * Used by: OpenRouter, Groq, DeepSeek, Together AI, Fireworks AI
 * All support POST /chat/completions with Bearer token auth.
 */
interface AiApiService {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://javis-launcher.app",
        @Header("X-Title") title: String = "JAVIS Launcher",
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

/**
 * ElevenLabs TTS API.
 * Base URL: https://api.elevenlabs.io/v1/
 * Auth: xi-api-key header
 */
interface ElevenLabsApiService {
    @POST("text-to-speech/{voiceId}")
    suspend fun textToSpeech(
        @Path("voiceId") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: TtsRequest
    ): Response<ResponseBody>

    @GET("voices")
    suspend fun getVoices(
        @Header("xi-api-key") apiKey: String
    ): Response<VoicesResponse>
}
