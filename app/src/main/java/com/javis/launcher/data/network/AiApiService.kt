package com.javis.launcher.data.network

import com.javis.launcher.data.model.ChatRequest
import com.javis.launcher.data.model.ChatResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AiApiService {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://javis-launcher.app",
        @Header("X-Title") title: String = "JAVIS Launcher",
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

interface ElevenLabsApiService {
    @POST("text-to-speech/{voiceId}")
    suspend fun textToSpeech(
        @Path("voiceId") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: com.javis.launcher.data.model.TtsRequest
    ): Response<ResponseBody>

    @GET("voices")
    suspend fun getVoices(
        @Header("xi-api-key") apiKey: String
    ): Response<com.javis.launcher.data.model.VoicesResponse>
}
