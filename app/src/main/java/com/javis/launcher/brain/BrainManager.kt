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
    private val _currentProvider = MutableStateFlow(AiProvider.GROQ)
    val currentProvider: StateFlow<AiProvider> = _currentProvider.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val conversationHistory = mutableListOf<ChatMessage>()

    private val providerPriority = listOf(
        AiProvider.GROQ,
        AiProvider.OPENROUTER,
        AiProvider.DEEPSEEK,
        AiProvider.TOGETHER,
        AiProvider.FIREWORKS
    )

    suspend fun chat(userMessage: String): Result<String> {
        _isThinking.value = true
        conversationHistory.add(ChatMessage("user", userMessage))

        conversationDao.insert(
            ConversationEntity(role = "user", content = userMessage, provider = _currentProvider.value.name)
        )

        val systemPrompt = buildSystemPrompt()
        val messages = listOf(ChatMessage("system", systemPrompt)) + conversationHistory.takeLast(12)

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

        return Result.success(getOfflineResponse(messages.lastOrNull { it.role == "user" }?.content ?: ""))
    }

    private suspend fun callProvider(
        provider: AiProvider,
        apiKey: String,
        messages: List<ChatMessage>
    ): Result<String> = try {
        val service = getService(provider)
        val model = getModel(provider)
        val request = ChatRequest(model = model, messages = messages, max_tokens = 512, temperature = 0.75f)
        val response = service.chatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )
        if (response.isSuccessful) {
            val content = response.body()?.choices?.firstOrNull()?.message?.content
            if (!content.isNullOrBlank()) Result.success(content.trim())
            else Result.failure(Exception("Empty response"))
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun buildSystemPrompt(): String {
        val nickname = prefs.getString("user_nickname", "boss") ?: "boss"
        val userName = prefs.getString("user_name", "") ?: ""
        val personalityMode = prefs.getString("personality_mode", "JAVIS") ?: "JAVIS"

        return """You are JAVIS — a brilliant, loyal AI assistant built into an Android launcher. You're sharp, efficient, and occasionally witty like Tony Stark's J.A.R.V.I.S.

User's name: ${if (userName.isNotBlank()) userName else "unknown"}
Address them as: "$nickname"
Personality: $personalityMode

## CRITICAL RULES FOR DEVICE ACTIONS:

When the user asks you to perform a device action, you MUST output a valid JSON action block.
The JSON block MUST come FIRST, on its own line, before your spoken response.

### JSON Format:
{"action":"ACTION_NAME","params":{...}}

### Available Actions & JSON formats:

OPEN APP:
{"action":"OPEN_APP","params":{"package":"com.whatsapp","name":"WhatsApp"}}

CALL SOMEONE:
{"action":"CALL_CONTACT","params":{"phone":"+1234567890","name":"John"}}

SET ALARM:
{"action":"SET_ALARM","params":{"time":"HH:MM","message":"label","repeat":false}}
Example for 8pm: {"action":"SET_ALARM","params":{"time":"20:00","message":"JAVIS Alarm","repeat":false}}
Example for 7:30am: {"action":"SET_ALARM","params":{"time":"07:30","message":"Morning alarm","repeat":false}}

SET TIMER:
{"action":"SET_TIMER","params":{"seconds":300}}

SEARCH WEB:
{"action":"SEARCH_WEB","params":{"query":"search term"}}

SEND SMS:
{"action":"SEND_SMS","params":{"phone":"number","message":"text"}}

WHATSAPP:
{"action":"OPEN_WHATSAPP","params":{"phone":"number","message":"text"}}
To just open WhatsApp: {"action":"OPEN_WHATSAPP","params":{}}

OPEN SETTINGS:
{"action":"OPEN_SETTINGS","params":{"page":"wifi"}}
Pages: wifi, bluetooth, display, sound, battery, location, notification

PLAY MUSIC:
{"action":"PLAY_MUSIC","params":{"query":"artist or song name"}}

### Common app package names:
- WhatsApp: com.whatsapp
- Instagram: com.instagram.android
- TikTok: com.zhiliaoapp.musically
- YouTube: com.google.android.youtube
- Chrome: com.android.chrome
- Camera: com.android.camera (or com.google.android.GoogleCamera)
- Gallery: com.android.gallery3d (or com.google.android.apps.photos)
- Maps: com.google.android.apps.maps
- Gmail: com.google.android.gm
- Calculator: com.android.calculator2
- Spotify: com.spotify.music
- Twitter/X: com.twitter.android
- Facebook: com.facebook.katana
- Snapchat: com.snapchat.android
- Netflix: com.netflix.mediaclient
- Telegram: org.telegram.messenger

## RESPONSE RULES:
- Put the JSON block on the FIRST line, then your spoken reply on the next line
- Your spoken reply should be SHORT (under 2 sentences) — you will be speaking it aloud
- NEVER show the JSON in your spoken reply
- NEVER say "I'll prepare" or "shall I send" — just DO it and confirm briefly
- If no action needed, just respond naturally and concisely
- Always address user as "$nickname"
- Be confident, capable, and occasionally witty

Example:
User: "Open WhatsApp"
You respond:
{"action":"OPEN_APP","params":{"package":"com.whatsapp","name":"WhatsApp"}}
Opening WhatsApp for you, $nickname.

Example:
User: "Set alarm for 8pm"
You respond:
{"action":"SET_ALARM","params":{"time":"20:00","message":"JAVIS Alarm","repeat":false}}
Alarm set for 8 PM, $nickname. Don't say I never do anything for you.""".trimIndent()
    }

    private fun getOfflineResponse(input: String): String {
        val lower = input.lowercase()
        val nickname = prefs.getString("user_nickname", "boss") ?: "boss"
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello, $nickname. Operating in offline mode — I can still handle basic tasks."
            lower.contains("time") ->
                "It's ${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}, $nickname."
            lower.contains("open") || lower.contains("launch") -> {
                val app = lower.replace(Regex("open|launch|start|the|app|please"), "").trim()
                """{"action":"OPEN_APP","params":{"name":"$app"}}
On it, $nickname."""
            }
            lower.contains("whatsapp") ->
                """{"action":"OPEN_APP","params":{"package":"com.whatsapp"}}
Opening WhatsApp, $nickname."""
            lower.contains("call") -> {
                val name = lower.replace(Regex("call|please|now"), "").trim()
                """{"action":"CALL_CONTACT","params":{"name":"$name"}}
Initiating call, $nickname."""
            }
            lower.contains("alarm") || lower.contains("wake") ->
                """{"action":"SET_ALARM","params":{"time":"07:00","message":"JAVIS Alarm","repeat":false}}
Alarm set, $nickname."""
            else ->
                "I'm in offline mode, $nickname. I can still open apps, call contacts, set alarms, and search the web."
        }
    }

    private fun getService(provider: AiProvider): AiApiService = when (provider) {
        AiProvider.OPENROUTER -> openRouterService
        AiProvider.GROQ -> groqService
        AiProvider.DEEPSEEK -> deepSeekService
        AiProvider.TOGETHER -> togetherService
        AiProvider.FIREWORKS -> fireworksService
        AiProvider.OFFLINE -> groqService
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
        AiProvider.OPENROUTER -> prefs.getString("openrouter_model", "meta-llama/llama-3.3-70b-instruct:free")
            ?: "meta-llama/llama-3.3-70b-instruct:free"
        AiProvider.GROQ -> prefs.getString("groq_model", "llama-3.1-70b-versatile")
            ?: "llama-3.1-70b-versatile"
        AiProvider.DEEPSEEK -> prefs.getString("deepseek_model", "deepseek-chat")
            ?: "deepseek-chat"
        AiProvider.TOGETHER -> prefs.getString("together_model", "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo")
            ?: "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo"
        AiProvider.FIREWORKS -> prefs.getString("fireworks_model", "accounts/fireworks/models/llama-v3p1-70b-instruct")
            ?: "accounts/fireworks/models/llama-v3p1-70b-instruct"
        AiProvider.OFFLINE -> ""
    }

    private fun isProviderEnabled(provider: AiProvider): Boolean =
        prefs.getBoolean("${provider.name.lowercase()}_enabled", true)

    fun clearConversation() {
        conversationHistory.clear()
    }

    fun getConversationHistory(): List<ChatMessage> = conversationHistory.toList()

    suspend fun testProvider(provider: AiProvider): Result<String> {
        val apiKey = getApiKey(provider)
        if (apiKey.isBlank()) return Result.failure(Exception("No API key configured for ${provider.name}"))
        return callProvider(
            provider, apiKey,
            listOf(ChatMessage("user", "Say exactly: JAVIS online and operational."))
        )
    }
}
