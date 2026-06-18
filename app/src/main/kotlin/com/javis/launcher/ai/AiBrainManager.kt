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
    val role: String, // "user" | "assistant" | "system"
    val content: String
)

data class AiBrainState(
    val currentProvider: AiProvider = AiProvider.OPENROUTER,
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
        You are JAVIS, an advanced AI companion and personal assistant. 
        You are intelligent, helpful, friendly, and professional — like a real Jarvis assistant.
        You live on the user's Android phone and help with tasks, conversations, and daily needs.
        Keep responses concise but warm. Use the user's name when appropriate.
        When performing tasks, briefly narrate what you are doing.
        Never be robotic. Sound human, calm, and confident.
    """.trimIndent()

    suspend fun chat(
        messages: List<AiMessage>,
        systemContext: String = ""
    ): Result<String> {
        val fullSystem = if (systemContext.isBlank()) systemPrompt
        else "$systemPrompt\n\n$systemContext"

        val providerOrder = buildProviderOrder()
        var lastError: Exception? = null

        for (provider in providerOrder) {
            if (provider == AiProvider.OFFLINE) {
                return offlineEngine.chat(messages)
            }

            val result = runCatching {
                val start = System.currentTimeMillis()
                val response = callProvider(provider, messages, fullSystem)
                val elapsed = System.currentTimeMillis() - start
                _state.value = _state.value.copy(
                    currentProvider = provider,
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
    ): String {
        return when (provider) {
            AiProvider.OPENROUTER -> openRouterClient.chat(messages, system)
            AiProvider.GROQ -> groqClient.chat(messages, system)
            AiProvider.DEEPSEEK -> deepSeekClient.chat(messages, system)
            AiProvider.TOGETHER -> togetherClient.chat(messages, system)
            AiProvider.FIREWORKS -> fireworksClient.chat(messages, system)
            AiProvider.OFFLINE -> offlineEngine.chat(messages).getOrThrow()
        }
    }

    private suspend fun buildProviderOrder(): List<AiProvider> {
        val prefs = preferences.getAiProviderPrefs()
        val ordered = mutableListOf<AiProvider>()
        if (prefs.openRouterKey.isNotBlank() && prefs.openRouterEnabled) ordered += AiProvider.OPENROUTER
        if (prefs.groqKey.isNotBlank() && prefs.groqEnabled) ordered += AiProvider.GROQ
        if (prefs.deepSeekKey.isNotBlank() && prefs.deepSeekEnabled) ordered += AiProvider.DEEPSEEK
        if (prefs.togetherKey.isNotBlank() && prefs.togetherEnabled) ordered += AiProvider.TOGETHER
        if (prefs.fireworksKey.isNotBlank() && prefs.fireworksEnabled) ordered += AiProvider.FIREWORKS
        ordered += AiProvider.OFFLINE
        return ordered
    }
}
