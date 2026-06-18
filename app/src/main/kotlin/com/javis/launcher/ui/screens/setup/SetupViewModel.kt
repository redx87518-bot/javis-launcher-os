package com.javis.launcher.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.data.db.dao.UserProfileDao
import com.javis.launcher.data.db.entity.UserProfileEntity
import com.javis.launcher.data.preferences.AiProviderPrefs
import com.javis.launcher.data.preferences.JavisPreferences
import com.javis.launcher.data.preferences.VoicePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val step: Int = 0,
    val name: String = "",
    val nickname: String = "",
    val openRouterKey: String = "",
    val groqKey: String = "",
    val elevenLabsKey: String = "",
    val setupComplete: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val preferences: JavisPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = userProfileDao.getProfile()
            if (profile?.setupComplete == true) {
                _uiState.update { it.copy(setupComplete = true) }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onNicknameChange(v: String) = _uiState.update { it.copy(nickname = v) }
    fun onOpenRouterKeyChange(v: String) = _uiState.update { it.copy(openRouterKey = v) }
    fun onGroqKeyChange(v: String) = _uiState.update { it.copy(groqKey = v) }
    fun onElevenLabsKeyChange(v: String) = _uiState.update { it.copy(elevenLabsKey = v) }

    fun nextStep() {
        val current = _uiState.value.step
        if (current < 4) _uiState.update { it.copy(step = current + 1) }
    }

    fun prevStep() {
        val current = _uiState.value.step
        if (current > 0) _uiState.update { it.copy(step = current - 1) }
    }

    fun completeSetup() {
        viewModelScope.launch {
            val s = _uiState.value
            userProfileDao.upsert(UserProfileEntity(
                name = s.name.ifBlank { "Sir" },
                nickname = s.nickname,
                setupComplete = true
            ))
            if (s.openRouterKey.isNotBlank() || s.groqKey.isNotBlank()) {
                preferences.saveAiProviderPrefs(AiProviderPrefs(
                    openRouterKey = s.openRouterKey,
                    groqKey = s.groqKey
                ))
            }
            if (s.elevenLabsKey.isNotBlank()) {
                preferences.saveVoicePrefs(VoicePrefs(elevenLabsKey = s.elevenLabsKey))
            }
            preferences.markSetupDone()
            _uiState.update { it.copy(setupComplete = true) }
        }
    }
}
