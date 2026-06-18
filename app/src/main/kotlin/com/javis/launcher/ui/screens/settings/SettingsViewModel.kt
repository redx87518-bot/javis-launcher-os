package com.javis.launcher.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.data.db.dao.MemoryDao
import com.javis.launcher.data.db.dao.UserProfileDao
import com.javis.launcher.data.db.entity.UserProfileEntity
import com.javis.launcher.data.preferences.AiProviderPrefs
import com.javis.launcher.data.preferences.JavisPreferences
import com.javis.launcher.data.preferences.VoicePrefs
import com.javis.launcher.service.JavisAccessibilityService
import com.javis.launcher.service.JavisNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Profile
    val name: String = "", val nickname: String = "",
    // AI Providers
    val openRouterKey: String = "", val openRouterModel: String = "", val openRouterEnabled: Boolean = true,
    val groqKey: String = "", val groqModel: String = "", val groqEnabled: Boolean = true,
    val deepSeekKey: String = "", val deepSeekModel: String = "", val deepSeekEnabled: Boolean = true,
    val togetherKey: String = "", val togetherModel: String = "", val togetherEnabled: Boolean = true,
    val fireworksKey: String = "", val fireworksModel: String = "", val fireworksEnabled: Boolean = true,
    // Voice
    val elevenLabsKey: String = "", val voiceId: String = "", val voiceSpeed: Float = 1.0f,
    val voiceStability: Float = 0.5f, val useTts: Boolean = false,
    // Greeting
    val greetingEnabled: Boolean = true, val voiceGreetingEnabled: Boolean = true,
    val briefingEnabled: Boolean = true,
    // Permissions
    val hasNotificationAccess: Boolean = false, val hasAccessibility: Boolean = false,
    val hasContacts: Boolean = false, val hasMicrophone: Boolean = false, val hasPhone: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: JavisPreferences,
    private val userProfileDao: UserProfileDao,
    private val memoryDao: MemoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            val profile = userProfileDao.getProfile()
            val aiPrefs = preferences.getAiProviderPrefs()
            val voicePrefs = preferences.getVoicePrefs()
            _uiState.update {
                it.copy(
                    name = profile?.name ?: "", nickname = profile?.nickname ?: "",
                    openRouterKey = aiPrefs.openRouterKey, openRouterModel = aiPrefs.openRouterModel,
                    openRouterEnabled = aiPrefs.openRouterEnabled,
                    groqKey = aiPrefs.groqKey, groqModel = aiPrefs.groqModel, groqEnabled = aiPrefs.groqEnabled,
                    deepSeekKey = aiPrefs.deepSeekKey, deepSeekModel = aiPrefs.deepSeekModel,
                    deepSeekEnabled = aiPrefs.deepSeekEnabled,
                    togetherKey = aiPrefs.togetherKey, togetherModel = aiPrefs.togetherModel,
                    togetherEnabled = aiPrefs.togetherEnabled,
                    fireworksKey = aiPrefs.fireworksKey, fireworksModel = aiPrefs.fireworksModel,
                    fireworksEnabled = aiPrefs.fireworksEnabled,
                    elevenLabsKey = voicePrefs.elevenLabsKey, voiceId = voicePrefs.voiceId,
                    voiceSpeed = voicePrefs.voiceSpeed, voiceStability = voicePrefs.voiceStability,
                    useTts = voicePrefs.useTts,
                    greetingEnabled = profile?.greetingEnabled ?: true,
                    voiceGreetingEnabled = profile?.voiceGreetingEnabled ?: true,
                    briefingEnabled = profile?.briefingEnabled ?: true,
                    hasNotificationAccess = isNotificationListenerEnabled(),
                    hasAccessibility = JavisAccessibilityService.isEnabled(),
                    hasContacts = hasPermission(Manifest.permission.READ_CONTACTS),
                    hasMicrophone = hasPermission(Manifest.permission.RECORD_AUDIO),
                    hasPhone = hasPermission(Manifest.permission.CALL_PHONE)
                )
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onNicknameChange(v: String) = _uiState.update { it.copy(nickname = v) }
    fun onOpenRouterKeyChange(v: String) = _uiState.update { it.copy(openRouterKey = v) }
    fun onOpenRouterModelChange(v: String) = _uiState.update { it.copy(openRouterModel = v) }
    fun toggleOpenRouter(v: Boolean) = _uiState.update { it.copy(openRouterEnabled = v) }
    fun onGroqKeyChange(v: String) = _uiState.update { it.copy(groqKey = v) }
    fun onGroqModelChange(v: String) = _uiState.update { it.copy(groqModel = v) }
    fun toggleGroq(v: Boolean) = _uiState.update { it.copy(groqEnabled = v) }
    fun onDeepSeekKeyChange(v: String) = _uiState.update { it.copy(deepSeekKey = v) }
    fun onDeepSeekModelChange(v: String) = _uiState.update { it.copy(deepSeekModel = v) }
    fun toggleDeepSeek(v: Boolean) = _uiState.update { it.copy(deepSeekEnabled = v) }
    fun onTogetherKeyChange(v: String) = _uiState.update { it.copy(togetherKey = v) }
    fun onTogetherModelChange(v: String) = _uiState.update { it.copy(togetherModel = v) }
    fun toggleTogether(v: Boolean) = _uiState.update { it.copy(togetherEnabled = v) }
    fun onFireworksKeyChange(v: String) = _uiState.update { it.copy(fireworksKey = v) }
    fun onFireworksModelChange(v: String) = _uiState.update { it.copy(fireworksModel = v) }
    fun toggleFireworks(v: Boolean) = _uiState.update { it.copy(fireworksEnabled = v) }
    fun onElevenLabsKeyChange(v: String) = _uiState.update { it.copy(elevenLabsKey = v) }
    fun onVoiceIdChange(v: String) = _uiState.update { it.copy(voiceId = v) }
    fun onVoiceSpeedChange(v: Float) = _uiState.update { it.copy(voiceSpeed = v) }
    fun toggleTts(v: Boolean) = _uiState.update { it.copy(useTts = v) }
    fun toggleGreeting(v: Boolean) = _uiState.update { it.copy(greetingEnabled = v) }
    fun toggleVoiceGreeting(v: Boolean) = _uiState.update { it.copy(voiceGreetingEnabled = v) }
    fun toggleBriefing(v: Boolean) = _uiState.update { it.copy(briefingEnabled = v) }

    fun saveProfile() = viewModelScope.launch {
        val s = _uiState.value
        val existing = userProfileDao.getProfile() ?: UserProfileEntity()
        userProfileDao.upsert(existing.copy(name = s.name, nickname = s.nickname))
    }

    fun saveAiSettings() = viewModelScope.launch {
        val s = _uiState.value
        preferences.saveAiProviderPrefs(AiProviderPrefs(
            openRouterKey = s.openRouterKey, openRouterModel = s.openRouterModel, openRouterEnabled = s.openRouterEnabled,
            groqKey = s.groqKey, groqModel = s.groqModel, groqEnabled = s.groqEnabled,
            deepSeekKey = s.deepSeekKey, deepSeekModel = s.deepSeekModel, deepSeekEnabled = s.deepSeekEnabled,
            togetherKey = s.togetherKey, togetherModel = s.togetherModel, togetherEnabled = s.togetherEnabled,
            fireworksKey = s.fireworksKey, fireworksModel = s.fireworksModel, fireworksEnabled = s.fireworksEnabled
        ))
    }

    fun saveVoiceSettings() = viewModelScope.launch {
        val s = _uiState.value
        preferences.saveVoicePrefs(VoicePrefs(
            elevenLabsKey = s.elevenLabsKey, voiceId = s.voiceId,
            voiceSpeed = s.voiceSpeed, voiceStability = s.voiceStability, useTts = s.useTts
        ))
    }

    fun saveGreetingSettings() = viewModelScope.launch {
        val s = _uiState.value
        val existing = userProfileDao.getProfile() ?: UserProfileEntity()
        userProfileDao.upsert(existing.copy(
            greetingEnabled = s.greetingEnabled,
            voiceGreetingEnabled = s.voiceGreetingEnabled,
            briefingEnabled = s.briefingEnabled
        ))
    }

    fun clearMemories() = viewModelScope.launch { memoryDao.deleteAll() }

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = android.provider.Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(context.packageName)
    }
}
