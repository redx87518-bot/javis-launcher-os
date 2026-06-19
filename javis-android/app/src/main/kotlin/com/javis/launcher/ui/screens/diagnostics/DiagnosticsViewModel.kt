package com.javis.launcher.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.ai.AiBrainManager
import com.javis.launcher.ai.AiProvider
import com.javis.launcher.data.preferences.JavisPreferences
import com.javis.launcher.voice.ElevenLabsStatus
import com.javis.launcher.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderTestResult(
    val provider: AiProvider,
    val status: String = "Not tested",
    val isLoading: Boolean = false,
    val isSuccess: Boolean? = null
)

data class DiagnosticsUiState(
    val voiceProvider: String = "Android TTS",
    val elevenLabsStatus: ElevenLabsStatus = ElevenLabsStatus(),
    val elevenLabsTestResult: String = "",
    val elevenLabsTestLoading: Boolean = false,
    val currentAiProvider: String = "Unknown",
    val lastAiResponseMs: Long = 0,
    val providerTests: Map<AiProvider, ProviderTestResult> = emptyMap(),
    val groqKey: String = "",
    val openRouterKey: String = "",
    val deepSeekKey: String = "",
    val togetherKey: String = "",
    val fireworksKey: String = "",
    val elevenLabsKey: String = "",
    val voiceId: String = ""
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val voiceManager: VoiceManager,
    private val brainManager: AiBrainManager,
    private val preferences: JavisPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val aiPrefs = preferences.getAiProviderPrefs()
            val voicePrefs = preferences.getVoicePrefs()
            _uiState.update {
                it.copy(
                    groqKey = aiPrefs.groqKey,
                    openRouterKey = aiPrefs.openRouterKey,
                    deepSeekKey = aiPrefs.deepSeekKey,
                    togetherKey = aiPrefs.togetherKey,
                    fireworksKey = aiPrefs.fireworksKey,
                    elevenLabsKey = voicePrefs.elevenLabsKey,
                    voiceId = voicePrefs.voiceId
                )
            }
            brainManager.state.collect { s ->
                _uiState.update {
                    it.copy(
                        currentAiProvider = s.currentProvider.displayName,
                        lastAiResponseMs = s.lastResponseMs
                    )
                }
            }
        }
        refreshVoiceStatus()
    }

    private fun refreshVoiceStatus() {
        val status = voiceManager.elevenLabsStatus
        _uiState.update {
            it.copy(
                elevenLabsStatus = status,
                voiceProvider = status.currentProvider
            )
        }
    }

    fun testElevenLabs() {
        val s = _uiState.value
        if (s.elevenLabsKey.isBlank()) {
            _uiState.update { it.copy(elevenLabsTestResult = "❌ No ElevenLabs API key configured") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(elevenLabsTestLoading = true, elevenLabsTestResult = "Testing connection…") }
            val result = voiceManager.elevenLabsClient.testConnection(s.elevenLabsKey, s.voiceId.ifBlank { "EXAVITQu4vr4xnSDxMaL" })
            _uiState.update {
                it.copy(
                    elevenLabsTestLoading = false,
                    elevenLabsTestResult = result.getOrElse { e -> "❌ ${e.message}" }
                )
            }
            refreshVoiceStatus()
        }
    }

    fun testVoiceSpeak() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(elevenLabsTestLoading = true, elevenLabsTestResult = "Playing test audio…") }
            val result = voiceManager.elevenLabsClient.testSpeak(
                s.elevenLabsKey, s.voiceId.ifBlank { "EXAVITQu4vr4xnSDxMaL" }
            )
            _uiState.update {
                it.copy(
                    elevenLabsTestLoading = false,
                    elevenLabsTestResult = result.getOrElse { e -> "❌ ${e.message}" }
                )
            }
            refreshVoiceStatus()
        }
    }

    fun testProvider(provider: AiProvider) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(providerTests = it.providerTests + (provider to ProviderTestResult(provider, "Testing…", isLoading = true)))
            }
            val result = brainManager.testProvider(provider)
            _uiState.update {
                it.copy(providerTests = it.providerTests + (provider to ProviderTestResult(
                    provider = provider,
                    status = result.getOrElse { e -> "❌ ${e.message?.take(80)}" },
                    isLoading = false,
                    isSuccess = result.isSuccess
                )))
            }
        }
    }
}
