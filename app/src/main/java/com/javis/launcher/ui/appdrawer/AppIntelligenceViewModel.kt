package com.javis.launcher.ui.appdrawer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.brain.BrainManager
import com.javis.launcher.data.local.AppDao
import com.javis.launcher.data.model.InstalledAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppIntelligenceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val brainManager: BrainManager
) : ViewModel() {

    private val _app = MutableStateFlow<InstalledAppEntity?>(null)
    val app: StateFlow<InstalledAppEntity?> = _app.asStateFlow()

    private val _explanation = MutableStateFlow("")
    val explanation: StateFlow<String> = _explanation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(packageName: String) {
        viewModelScope.launch {
            _app.value = appDao.getAppByPackage(packageName)
        }
    }

    fun explain() {
        val a = _app.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _explanation.value = ""
            val prompt = buildPrompt(a)
            val result = brainManager.chat(prompt)
            _explanation.value = result.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "I couldn't analyze this app right now. You can still open it or ask me a question about it."
            _isLoading.value = false
        }
    }

    private fun buildPrompt(a: InstalledAppEntity): String = """
        You are JAVIS, an Android launcher assistant.
        The user is examining the app "${a.appName}" (package ${a.packageName}, category ${a.category}).
        Using only reliable public knowledge, explain in under 120 words:
        - What the app does
        - Its main functions
        - Whether it appears trustworthy
        Do not invent permissions or claim the app is malicious without evidence.
    """.trimIndent()

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) context.startActivity(intent)
                appDao.incrementLaunch(packageName)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun toggleFavorite(packageName: String, current: Boolean) {
        viewModelScope.launch { appDao.setFavorite(packageName, !current) }
    }
}
