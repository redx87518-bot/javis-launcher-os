package com.javis.launcher.ui.screens.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.agents.AgentRouter
import com.javis.launcher.ai.AiBrainManager
import com.javis.launcher.ai.AiMessage
import com.javis.launcher.ai.AiProvider
import com.javis.launcher.ai.TaskPlanner
import com.javis.launcher.agents.MemoryAgent
import com.javis.launcher.data.db.dao.ConversationDao
import com.javis.launcher.data.db.dao.MessageDao
import com.javis.launcher.data.db.entity.ConversationEntity
import com.javis.launcher.data.db.entity.MessageEntity
import com.javis.launcher.voice.VoiceManager
import com.javis.launcher.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val voiceState: VoiceState = VoiceState.IDLE,
    val providerName: String = "OpenRouter",
    val isOnline: Boolean = true
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val brainManager: AiBrainManager,
    private val taskPlanner: TaskPlanner,
    private val agentRouter: AgentRouter,
    private val memoryAgent: MemoryAgent,
    val voiceManager: VoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var conversationId: Long = -1L
    private val contextMessages = mutableListOf<AiMessage>()

    init {
        initConversation()
        observeVoiceState()
        observeAiBrainState()
    }

    private fun initConversation() {
        viewModelScope.launch {
            val existing = conversationDao.getLatest()
            conversationId = if (existing != null) {
                existing.id
            } else {
                conversationDao.upsert(ConversationEntity(title = "New Conversation"))
            }
            messageDao.observeByConversation(conversationId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
                contextMessages.clear()
                contextMessages.addAll(msgs.takeLast(20).map { AiMessage(it.role, it.content) })
            }
        }
    }

    private fun observeVoiceState() {
        viewModelScope.launch {
            voiceManager.state.collect { state ->
                _uiState.update { it.copy(voiceState = state) }
            }
        }
    }

    private fun observeAiBrainState() {
        viewModelScope.launch {
            brainManager.state.collect { s ->
                _uiState.update { it.copy(
                    providerName = s.currentProvider.displayName,
                    isOnline = s.currentProvider != AiProvider.OFFLINE
                )}
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        _uiState.update { it.copy(inputText = "") }
        processInput(text, isVoice = false)
    }

    fun toggleVoice() {
        if (_uiState.value.voiceState == VoiceState.LISTENING) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening { transcript ->
                if (transcript.isNotBlank()) processInput(transcript, isVoice = true)
                else voiceManager.setIdle()
            }
        }
    }

    private fun processInput(input: String, isVoice: Boolean) {
        viewModelScope.launch {
            // Save user message
            val userMsgId = messageDao.insert(
                MessageEntity(conversationId = conversationId, role = "user",
                    content = input, isVoice = isVoice)
            )
            conversationDao.incrementMessages(conversationId)

            _uiState.update { it.copy(isProcessing = true) }

            // Extract memories
            memoryAgent.extractAndStoreFromConversation(input)

            // Plan and execute
            val plan = taskPlanner.plan(input)
            val agentResults = if (plan.steps.any { it.agentType != com.javis.launcher.ai.AgentType.CONVERSATION }) {
                agentRouter.execute(plan)
            } else emptyList()

            val context = if (agentResults.isNotEmpty()) {
                "Task results:\n" + agentResults.joinToString("\n") {
                    if (it.success) "✓ ${it.message}" else "✗ ${it.message}"
                } + "\n\nNarrate naturally in 1-2 sentences."
            } else ""

            contextMessages.add(AiMessage("user", input))
            val result = brainManager.chat(contextMessages, context)
            val response = result.getOrElse { "I had a small hiccup. Ask me again?" }
            contextMessages.add(AiMessage("assistant", response))
            if (contextMessages.size > 20) {
                contextMessages.removeAt(0)
                if (contextMessages.isNotEmpty()) contextMessages.removeAt(0)
            }

            messageDao.insert(
                MessageEntity(conversationId = conversationId, role = "assistant",
                    content = response, provider = brainManager.state.value.currentProvider.displayName)
            )
            conversationDao.incrementMessages(conversationId)

            _uiState.update { it.copy(isProcessing = false) }
            if (isVoice) voiceManager.speak(response)
        }
    }
}
