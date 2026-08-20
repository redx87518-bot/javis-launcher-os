package com.javis.launcher.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.accessibility.JavisAccessibilityService
import com.javis.launcher.brain.BrainManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenUnderstandingViewModel @Inject constructor(
    private val brainManager: BrainManager
) : ViewModel() {

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _interpretation = MutableStateFlow("")
    val interpretation: StateFlow<String> = _interpretation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasService = MutableStateFlow(JavisAccessibilityService.instance != null)
    val hasService: StateFlow<Boolean> = _hasService.asStateFlow()

    fun capture() {
        val svc = JavisAccessibilityService.instance
        _hasService.value = svc != null
        _interpretation.value = ""
        _description.value = svc?.describeScreen()
            ?: "Accessibility service is not connected. Enable JAVIS in Android Accessibility settings to read the current screen."
    }

    fun interpret() {
        if (_description.value.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val prompt = """
                You are JAVIS analyzing an Android screen via its accessibility tree.
                Summarize what is on screen, list the key visible elements (buttons, inputs, text),
                and identify the most likely primary action. Keep it under 120 words. Never invent elements.
                
                Screen:
                ${_description.value}
            """.trimIndent()
            val result = brainManager.chat(prompt)
            _interpretation.value = result.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "I couldn't interpret the screen right now."
            _isLoading.value = false
        }
    }
}
