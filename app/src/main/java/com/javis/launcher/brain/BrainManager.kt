package com.javis.launcher.brain

import android.content.Context
import android.content.SharedPreferences
import com.javis.launcher.data.local.ConversationDao
import com.javis.launcher.data.local.MemoryDao
import com.javis.launcher.data.model.*
import com.javis.launcher.data.network.AiApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BrainManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("openrouter") private val openRouterService: AiApiService,
    @Named("groq") private val groqService: AiApiService,
    @Named("deepseek") private val deepSeekService: AiApiService,
    @Named("together") private val togetherService: AiApiService,
    @Named("fireworks") private val fireworksService: AiApiService,
    private val conversationDao: ConversationDao,
    private val memoryDao: MemoryDao,
    private val prefs: SharedPreferences
) {
    private val _currentProvider = MutableStateFlow(AiProvider.OPENROUTER)
    val currentProvider: StateFlow<AiProvider> = _currentProvider.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val conversationHistory = mutableListOf<ChatMessage>()

    private val providerPriority = listOf(
        AiProvider.OPENROUTER,
        AiProvider.GROQ,
        AiProvider.DEEPSEEK,
        AiProvider.TOGETHER,
        AiProvider.FIREWORKS
    )

    suspend fun chat(userMessage: String): Result<String> {
        _isThinking.value = true
        conversationHistory.add(ChatMessage("user", userMessage))

        // Save to DB
        conversationDao.insert(
            ConversationEntity(role = "user", content = userMessage, provider = _currentProvider.value.name)
        )

        val systemPrompt = buildSystemPrompt()
        val messages = listOf(ChatMessage("system", systemPrompt)) + conversationHistory.takeLast(10)

        val result = tryProvidersInOrder(messages)
        _isThinking.value = false
        return result
    }

    private suspend fun tryProvidersInOrder(messages: List<ChatMessage>): Result<String> {
        val startIndex = providerPriority.indexOf(_currentProvider.value).coerceAtLeast(0)
        val ordered = providerPriority.drop(startIndex) + providerPriority.take(startIndex)

        for (provider in ordered) {
            val apiKey = getApiKey(provider)
            if (apiKey.isBlank()) continue
            if (!isProviderEnabled(provider)) continue

            val result = callProvider(provider, apiKey, messages)
            if (result.isSuccess) {
                _currentProvider.value = provider
                val response = result.getOrNull() ?: continue
                conversationHistory.add(ChatMessage("assistant", response))
                conversationDao.insert(
                    ConversationEntity(role = "assistant", content = response, provider = provider.name)
                )
                return result
            }
        }

        // Fallback offline response
        return Result.success(getOfflineResponse(messages.lastOrNull { it.role == "user" }?.content ?: ""))
    }

    private suspend fun callProvider(
        provider: AiProvider,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> = try {
        val service = getService(provider)
        val model = getModel(provider)
        val request = ChatRequest(model = model, messages = messages, max_tokens = 1024)
        val response = service.chatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )
        if (response.isSuccessful) {
            val content = response.body()?.choices?.firstOrNull()?.message?.content
            if (!content.isNullOrBlank()) Result.success(content)
            else Result.failure(Exception("Empty response"))
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun buildSystemPrompt(): String {
        val nickname = prefs.getString("user_nickname", "Sir") ?: "Sir"
        val userName = prefs.getString("user_name", "") ?: ""
        val personalityMode = prefs.getString("personality_mode", "JARVIS") ?: "JARVIS"
        val memories = "" // Could load top memories from DB here

        return """You are JAVIS, an advanced AI assistant and Android launcher. You are intelligent, professional, friendly, and occasionally humorous.
You address the user as "$nickname"${if (userName.isNotBlank()) " (their name is $userName)" else ""}.

Personality mode: $personalityMode

Key behaviors:
- Be concise but complete in responses
- For device actions (opening apps, calling contacts, setting alarms), respond with a JSON action block followed by a confirmation message
- Format: {"action":"OPEN_APP","params":{"package":"com.example.app"}} 
- Available actions: OPEN_APP, CALL_CONTACT, SEND_SMS, SET_ALARM, SET_REMINDER, SEARCH_WEB, SEARCH_CONTACTS
- Always confirm before sending messages: "I've prepared the message: [msg]. Shall I send it?"
- Reference context and memory when appropriate
- Keep responses under 150 words unless the user asks for detail

$memories""".trimIndent()
    }

    private fun getOfflineResponse(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") ->
                "Hello, Sir. I'm currently operating in offline mode. Basic commands are available."
            lower.contains("time") ->
                "It's ${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}."
            lower.contains("open") ->
                """{"action":"OPEN_APP","params":{"query":"${lower.replace("open","").trim()}"}} Opening the app for you, Sir."""
            lower.contains("call") ->
                """{"action":"SEARCH_CONTACTS","params":{"query":"${lower.replace("call","").trim()}"}} Searching contacts, Sir."""
            else -> "I'm in offline mode, Sir. I can still open apps, search contacts, and handle basic tasks."
        }
    }

    private fun getService(provider: AiProvider): AiApiService = when (provider) {
        AiProvider.OPENROUTER -> openRouterService
        AiProvider.GROQ -> groqService
        AiProvider.DEEPSEEK -> deepSeekService
        AiProvider.TOGETHER -> togetherService
        AiProvider.FIREWORKS -> fireworksService
        AiProvider.OFFLINE -> openRouterService
    }

    private fun getApiKey(provider: AiProvider): String = when (provider) {
        AiProvider.OPENROUTER -> prefs.getString("openrouter_api_key", "") ?: ""
        AiProvider.GROQ -> prefs.getString("groq_api_key", "") ?: ""
        AiProvider.DEEPSEEK -> prefs.getString("deepseek_api_key", "") ?: ""
        AiProvider.TOGETHER -> prefs.getString("together_api_key", "") ?: ""
        AiProvider.FIREWORKS -> prefs.getString("fireworks_api_key", "") ?: ""
        AiProvider.OFFLINE -> ""
    }

    private fun getModel(provider: AiProvider): String = when (provider) {
        AiProvider.OPENROUTER -> prefs.getString("openrouter_model", "meta-llama/llama-3.1-8b-instruct:free") ?: "meta-llama/llama-3.1-8b-instruct:free"
        AiProvider.GROQ -> prefs.getString("groq_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        AiProvider.DEEPSEEK -> prefs.getString("deepseek_model", "deepseek-chat") ?: "deepseek-chat"
        AiProvider.TOGETHER -> prefs.getString("together_model", "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo") ?: "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"
        AiProvider.FIREWORKS -> prefs.getString("fireworks_model", "accounts/fireworks/models/llama-v3p1-8b-instruct") ?: "accounts/fireworks/models/llama-v3p1-8b-instruct"
        AiProvider.OFFLINE -> ""
    }

    private fun isProviderEnabled(provider: AiProvider): Boolean =
        prefs.getBoolean("${provider.name.lowercase()}_enabled", true)

    fun clearConversation() {
        conversationHistory.clear()
    }

    suspend fun testProvider(provider: AiProvider): Result<String> {
        val apiKey = getApiKey(provider)
        if (apiKey.isBlank()) return Result.failure(Exception("No API key configured"))
        return callProvider(provider, apiKey, listOf(ChatMessage("user", "Say 'JAVIS online' in one sentence.")))
    }
}
