package com.javis.launcher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "javis_prefs")

data class AiProviderPrefs(
    val openRouterKey: String = "", val openRouterModel: String = "", val openRouterEnabled: Boolean = true,
    val groqKey: String = "", val groqModel: String = "", val groqEnabled: Boolean = true,
    val deepSeekKey: String = "", val deepSeekModel: String = "", val deepSeekEnabled: Boolean = true,
    val togetherKey: String = "", val togetherModel: String = "", val togetherEnabled: Boolean = true,
    val fireworksKey: String = "", val fireworksModel: String = "", val fireworksEnabled: Boolean = true
)

data class VoicePrefs(
    val elevenLabsKey: String = "",
    val voiceId: String = "EXAVITQu4vr4xnSDxMaL",
    val voiceSpeed: Float = 1.0f,
    val voiceStability: Float = 0.5f,
    val useTts: Boolean = false
)

@Singleton
class JavisPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    private object Keys {
        // AI Providers
        val OR_KEY = stringPreferencesKey("or_key")
        val OR_MODEL = stringPreferencesKey("or_model")
        val OR_ENABLED = booleanPreferencesKey("or_enabled")
        val GROQ_KEY = stringPreferencesKey("groq_key")
        val GROQ_MODEL = stringPreferencesKey("groq_model")
        val GROQ_ENABLED = booleanPreferencesKey("groq_enabled")
        val DS_KEY = stringPreferencesKey("ds_key")
        val DS_MODEL = stringPreferencesKey("ds_model")
        val DS_ENABLED = booleanPreferencesKey("ds_enabled")
        val TOGETHER_KEY = stringPreferencesKey("together_key")
        val TOGETHER_MODEL = stringPreferencesKey("together_model")
        val TOGETHER_ENABLED = booleanPreferencesKey("together_enabled")
        val FW_KEY = stringPreferencesKey("fw_key")
        val FW_MODEL = stringPreferencesKey("fw_model")
        val FW_ENABLED = booleanPreferencesKey("fw_enabled")
        // Voice
        val EL_KEY = stringPreferencesKey("el_key")
        val VOICE_ID = stringPreferencesKey("voice_id")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_STABILITY = floatPreferencesKey("voice_stability")
        val USE_TTS = booleanPreferencesKey("use_tts")
        // Misc
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val LAST_UNLOCK = longPreferencesKey("last_unlock")
        val GREETING_FREQUENCY = stringPreferencesKey("greeting_freq")
    }

    suspend fun getAiProviderPrefs(): AiProviderPrefs {
        val prefs = ds.data.first()
        return AiProviderPrefs(
            openRouterKey = prefs[Keys.OR_KEY] ?: "",
            openRouterModel = prefs[Keys.OR_MODEL] ?: "",
            openRouterEnabled = prefs[Keys.OR_ENABLED] ?: true,
            groqKey = prefs[Keys.GROQ_KEY] ?: "",
            groqModel = prefs[Keys.GROQ_MODEL] ?: "",
            groqEnabled = prefs[Keys.GROQ_ENABLED] ?: true,
            deepSeekKey = prefs[Keys.DS_KEY] ?: "",
            deepSeekModel = prefs[Keys.DS_MODEL] ?: "",
            deepSeekEnabled = prefs[Keys.DS_ENABLED] ?: true,
            togetherKey = prefs[Keys.TOGETHER_KEY] ?: "",
            togetherModel = prefs[Keys.TOGETHER_MODEL] ?: "",
            togetherEnabled = prefs[Keys.TOGETHER_ENABLED] ?: true,
            fireworksKey = prefs[Keys.FW_KEY] ?: "",
            fireworksModel = prefs[Keys.FW_MODEL] ?: "",
            fireworksEnabled = prefs[Keys.FW_ENABLED] ?: true
        )
    }

    suspend fun saveAiProviderPrefs(prefs: AiProviderPrefs) {
        ds.edit {
            it[Keys.OR_KEY] = prefs.openRouterKey
            it[Keys.OR_MODEL] = prefs.openRouterModel
            it[Keys.OR_ENABLED] = prefs.openRouterEnabled
            it[Keys.GROQ_KEY] = prefs.groqKey
            it[Keys.GROQ_MODEL] = prefs.groqModel
            it[Keys.GROQ_ENABLED] = prefs.groqEnabled
            it[Keys.DS_KEY] = prefs.deepSeekKey
            it[Keys.DS_MODEL] = prefs.deepSeekModel
            it[Keys.DS_ENABLED] = prefs.deepSeekEnabled
            it[Keys.TOGETHER_KEY] = prefs.togetherKey
            it[Keys.TOGETHER_MODEL] = prefs.togetherModel
            it[Keys.TOGETHER_ENABLED] = prefs.togetherEnabled
            it[Keys.FW_KEY] = prefs.fireworksKey
            it[Keys.FW_MODEL] = prefs.fireworksModel
            it[Keys.FW_ENABLED] = prefs.fireworksEnabled
        }
    }

    suspend fun getVoicePrefs(): VoicePrefs {
        val prefs = ds.data.first()
        return VoicePrefs(
            elevenLabsKey = prefs[Keys.EL_KEY] ?: "",
            voiceId = prefs[Keys.VOICE_ID] ?: "EXAVITQu4vr4xnSDxMaL",
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
            voiceStability = prefs[Keys.VOICE_STABILITY] ?: 0.5f,
            useTts = prefs[Keys.USE_TTS] ?: false
        )
    }

    suspend fun saveVoicePrefs(prefs: VoicePrefs) {
        ds.edit {
            it[Keys.EL_KEY] = prefs.elevenLabsKey
            it[Keys.VOICE_ID] = prefs.voiceId
            it[Keys.VOICE_SPEED] = prefs.voiceSpeed
            it[Keys.VOICE_STABILITY] = prefs.voiceStability
            it[Keys.USE_TTS] = prefs.useTts
        }
    }

    fun observeSetupDone(): Flow<Boolean> = ds.data.map { it[Keys.SETUP_DONE] ?: false }

    suspend fun markSetupDone() = ds.edit { it[Keys.SETUP_DONE] = true }

    suspend fun recordUnlock() = ds.edit { it[Keys.LAST_UNLOCK] = System.currentTimeMillis() }

    suspend fun getLastUnlock(): Long = ds.data.first()[Keys.LAST_UNLOCK] ?: 0L

    fun observeGreetingFrequency(): Flow<String> = ds.data.map { it[Keys.GREETING_FREQUENCY] ?: "every_unlock" }

    suspend fun saveGreetingFrequency(freq: String) = ds.edit { it[Keys.GREETING_FREQUENCY] = freq }
}
