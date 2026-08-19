package com.javis.launcher.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.brain.BrainManager
import com.javis.launcher.data.model.CoreState
import com.javis.launcher.tasks.TaskPlanner
import com.javis.launcher.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceModeViewModel @Inject constructor(
    private val brainManager: BrainManager,
    private val voiceManager: VoiceManager,
    private val taskPlanner: TaskPlanner
) : ViewModel() {

    private val _state = MutableStateFlow(CoreState.IDLE)
    val state: StateFlow<CoreState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _response = MutableStateFlow("Tap the mic and speak to JAVIS.")
    val response: StateFlow<String> = _response.asStateFlow()

    fun setListening() {
        _state.value = CoreState.LISTENING
        _transcript.value = ""
    }

    fun onPartialResult(text: String) {
        _transcript.value = text
    }

    fun onFinalResult(text: String) {
        if (text.isBlank()) {
            _state.value = CoreState.IDLE
            return
        }
        _transcript.value = text
        process(text)
    }

    fun onError() {
        _state.value = CoreState.ERROR
        _response.value = "I didn't catch that. Try again."
    }

    private fun process(text: String) {
        viewModelScope.launch {
            _state.value = CoreState.THINKING
            _response.value = "Processing..."
            val result = taskPlanner.processUserInput(text)
            _state.value = CoreState.SPEAKING
            _response.value = result
            voiceManager.speak(result)
            _state.value = CoreState.IDLE
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
        _state.value = CoreState.IDLE
    }

    fun setIdle() {
        if (_state.value != CoreState.SPEAKING) _state.value = CoreState.IDLE
    }
}
