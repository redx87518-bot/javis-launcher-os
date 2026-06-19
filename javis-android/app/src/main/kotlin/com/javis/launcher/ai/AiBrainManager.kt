package com.javis.launcher.ai

import com.javis.launcher.data.preferences.JavisPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AiProvider(val displayName: String, val priority: Int) {
    OPENROUTER("OpenRouter", 1),
    GROQ("Groq", 2),
    DEEPSEEK("DeepSeek", 3),
    TOGETHER("Together AI", 4),
    FIREWORKS("Fireworks AI", 5),
    OFFLINE("Offline", 99)
}

data class AiMessage(
    val role: String,
    val content: String
)

data class AiBrainState(
    val currentProvider: AiProvider = AiProvider.GROQ,
    val isOnline: Boolean = true,
    val lastResponseMs: Long = 0,
    val failedProviders: Set<AiProvider> = emptySet()
)

@Singleton
class AiBrainManager @Inject constructor(
    private val preferences: JavisPreferences,
    private val openRouterClient: OpenRouterClient,
    private val groqClient: GroqClient,
    private val deepSeekClient: DeepSeekClient,
    private val togetherClient: TogetherAiClient,
    private val fireworksClient: FireworksClient,
    private val offlineEngine: OfflineAiEngine
) {
    private val _state = MutableStateFlow(AiBrainState())
    val state: StateFlow<AiBrainState> = _state

    private val systemPrompt = """
        You are JAVIS — an advanced AI companion and personal assistant running on Android.
        You are intelligent, professional, friendly, and slightly witty — exactly like Jarvis from Iron Man.
        
        === CRITICAL RULES — NEVER BREAK THESE ===
        1. NEVER output JSON, XML, code blocks, or structured data formats.
        2. ALWAYS respond in natural spoken English — warm, calm, confident.
        3. Keep responses SHORT — 1 to 3 sentences maximum unless the user asks for detail.
        4. When a task was just executed, narrate the result naturally:
           ✓ "Opening WhatsApp now, Sir."
           ✓ "Alarm set for 7 AM. You'll be up bright and early."
           ✓ "Calling Musa now."
           ✗ Never say: {"action":"open","app":"WhatsApp"}
        5. Use the user's name or "Sir" when known.
        6. Sound human, never robotic.
        7. Be proactive — if relevant, suggest a follow-up.
        
        === YOUR CAPABILITIES ===
        You live on the user's Android phone. You can:
        - Launch any installed app
        - Call and message contacts
        - Set alarms, timers, reminders
        - Read and summarize notifications
        - Remember personal facts about the user
        - Search the web
        - Have full conversations
        
        When you are given task execution results, narrate them naturally as if you just performed the action.
        Never describe what you "would" do — describe what has been done.
    """.trimIndent()

    suspend fun chat(
        messages: List<AiMessage>,
        systemContext: String = "",
        userName: String = ""
    ): Result<String> {
        val nameContext = if (userName.isNotBlank() && userName != "Sir")
            "\nThe user's name is $userName. Address them by name occasionally."
        else ""

        val fullSystem = buildString {
            append(systemPrompt)
            append(nameContext)
            if (systemContext.isNotBlank()) {
                append("\n\n=== TASK CONTEXT ===\n")
                append(systemContext)
            }
        }

        val providerOrder = buildProviderOrder()
        var lastError: Exception? = null

        for (provider in providerOrder) {
            if (provider == AiProvider.OFFLINE) {
                _state.value = _state.value.copy(currentProvider = AiProvider.OFFLINE, isOnline = false)
                return offlineEngine.chat(messages)
            }

            val result = runCatching {
                val start = System.currentTimeMillis()
                val response = callProvider(provider, messages, fullSystem)
                val elapsed = System.currentTimeMillis() - start
                _state.value = _state.value.copy(
                    currentProvider = provider,
                    isOnline = true,
                    lastResponseMs = elapsed,
                    failedProviders = _state.value.failedProviders - provider
                )
                response
            }

            if (result.isSuccess) return result
            lastError = result.exceptionOrNull() as? Exception
            _state.value = _state.value.copy(
                failedProviders = _state.value.failedProviders + provider
            )
        }

        return Result.failure(lastError ?: Exception("All AI providers unavailable"))
    }

    private suspend fun callProvider(
        provider: AiProvider,
        messages: List<AiMessage>,
        system: String
    ): String = when (provider) {
        AiProvider.OPENROUTER -> openRouterClient.chat(messages, system)
        AiProvider.GROQ -> groqClient.chat(messages, system)
        AiProvider.DEEPSEEK -> deepSeekClient.chat(messages, system)
        AiProvider.TOGETHER -> togetherClient.chat(messages, system)
        AiProvider.FIREWORKS -> fireworksClient.chat(messages, system)
        AiProvider.OFFLINE -> offlineEngine.chat(messages).getOrThrow()
    }

    private suspend fun buildProviderOrder(): List<AiProvider> {
        val prefs = preferences.getAiProviderPrefs()
        val ordered = mutableListOf<AiProvider>()
        if (prefs.groqKey.isNotBlank() && prefs.groqEnabled) ordered += AiProvider.GROQ
        if (prefs.openRouterKey.isNotBlank() && prefs.openRouterEnabled) ordered += AiProvider.OPENROUTER
        if (prefs.deepSeekKey.isNotBlank() && prefs.deepSeekEnabled) ordered += AiProvider.DEEPSEEK
        if (prefs.togetherKey.isNotBlank() && prefs.togetherEnabled) ordered += AiProvider.TOGETHER
        if (prefs.fireworksKey.isNotBlank() && prefs.fireworksEnabled) ordered += AiProvider.FIREWORKS
        ordered += AiProvider.OFFLINE
        return ordered
    }

    suspend fun testProvider(provider: AiProvider): Result<String> {
        val testMsgs = listOf(AiMessage("user", "Say: JAVIS online. (exactly those words)"))
        return runCatching {
            val response = callProvider(provider, testMsgs, "You are a brief test assistant.")
            "✅ Connected — ${response.take(60)}"
        }.onFailure { e ->
            _state.value = _state.value.copy(failedProviders = _state.value.failedProviders + provider)
        }
    }
}
