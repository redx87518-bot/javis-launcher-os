package com.javis.launcher.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.brain.BrainManager
import com.javis.launcher.data.model.AiProvider
import com.javis.launcher.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val brainManager: BrainManager,
    private val voiceManager: VoiceManager
) : ViewModel() {

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    var testingProvider: AiProvider? = null

    // AI Providers
    fun getApiKey(provider: AiProvider): String =
        prefs.getString("${provider.name.lowercase()}_api_key", "") ?: ""

    fun setApiKey(provider: AiProvider, key: String) =
        prefs.edit().putString("${provider.name.lowercase()}_api_key", key).apply()

    fun getModel(provider: AiProvider): String =
        prefs.getString("${provider.name.lowercase()}_model", getDefaultModel(provider)) ?: getDefaultModel(provider)

    fun setModel(provider: AiProvider, model: String) =
        prefs.edit().putString("${provider.name.lowercase()}_model", model).apply()

    fun isProviderEnabled(provider: AiProvider): Boolean =
        prefs.getBoolean("${provider.name.lowercase()}_enabled", true)

    fun setProviderEnabled(provider: AiProvider, enabled: Boolean) =
        prefs.edit().putBoolean("${provider.name.lowercase()}_enabled", enabled).apply()

    fun testProvider(provider: AiProvider) {
        testingProvider = provider
        _isTesting.value = true
        _testResult.value = null
        viewModelScope.launch {
            val result = brainManager.testProvider(provider)
            _testResult.value = if (result.isSuccess) {
                "✓ Connected: ${result.getOrNull()?.take(80)}"
            } else {
                "✗ Failed: ${result.exceptionOrNull()?.message?.take(80)}"
            }
            _isTesting.value = false
        }
    }

    // ElevenLabs
    fun getElevenLabsKey(): String = prefs.getString("elevenlabs_api_key", "") ?: ""
    fun setElevenLabsKey(key: String) = prefs.edit().putString("elevenlabs_api_key", key).apply()
    fun getVoiceId(): String = prefs.getString("elevenlabs_voice_id", "pNInz6obpgDQGcFmaJgB") ?: "pNInz6obpgDQGcFmaJgB"
    fun setVoiceId(id: String) = prefs.edit().putString("elevenlabs_voice_id", id).apply()
    fun getVoiceSpeed(): Float = prefs.getFloat("voice_speed", 1.0f)
    fun setVoiceSpeed(speed: Float) = prefs.edit().putFloat("voice_speed", speed).apply()
    fun useElevenLabs(): Boolean = prefs.getBoolean("use_elevenlabs", true)
    fun setUseElevenLabs(use: Boolean) = prefs.edit().putBoolean("use_elevenlabs", use).apply()

    fun testVoice() {
        viewModelScope.launch {
            voiceManager.speak("Hello Sir, JAVIS voice system is operational.")
        }
    }

    // Profile
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun getNickname(): String = prefs.getString("user_nickname", "Sir") ?: "Sir"
    fun setNickname(nick: String) = prefs.edit().putString("user_nickname", nick).apply()

    // Personality
    fun getPersonalityMode(): String = prefs.getString("personality_mode", "JARVIS") ?: "JARVIS"
    fun setPersonalityMode(mode: String) = prefs.edit().putString("personality_mode", mode).apply()

    // Daily Briefing
    fun isDailyBriefingEnabled(): Boolean = prefs.getBoolean("daily_briefing_enabled", true)
    fun setDailyBriefingEnabled(enabled: Boolean) = prefs.edit().putBoolean("daily_briefing_enabled", enabled).apply()
    fun getBriefingFrequency(): String = prefs.getString("briefing_frequency", "FIRST_UNLOCK") ?: "FIRST_UNLOCK"
    fun setBriefingFrequency(freq: String) = prefs.edit().putString("briefing_frequency", freq).apply()

    private fun getDefaultModel(provider: AiProvider) = when (provider) {
        AiProvider.OPENROUTER -> "meta-llama/llama-3.1-8b-instruct:free"
        AiProvider.GROQ -> "llama-3.1-8b-instant"
        AiProvider.DEEPSEEK -> "deepseek-chat"
        AiProvider.TOGETHER -> "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo"
        AiProvider.FIREWORKS -> "accounts/fireworks/models/llama-v3p1-8b-instruct"
        AiProvider.OFFLINE -> ""
    }
}
