package com.javis.launcher.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.agents.AgentRouter
import com.javis.launcher.ai.AiBrainManager
import com.javis.launcher.ai.AiMessage
import com.javis.launcher.ai.TaskPlanner
import com.javis.launcher.agents.MemoryAgent
import com.javis.launcher.data.db.dao.InstalledAppDao
import com.javis.launcher.data.db.dao.NotificationDao
import com.javis.launcher.data.db.dao.ReminderDao
import com.javis.launcher.data.db.dao.UserProfileDao
import com.javis.launcher.data.db.entity.InstalledAppEntity
import com.javis.launcher.data.db.entity.NotificationEntity
import com.javis.launcher.data.db.entity.ReminderEntity
import com.javis.launcher.data.db.entity.UserProfileEntity
import com.javis.launcher.voice.VoiceManager
import com.javis.launcher.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "",
    val userName: String = "",
    val briefing: String = "",
    val favoriteApps: List<InstalledAppEntity> = emptyList(),
    val recentApps: List<InstalledAppEntity> = emptyList(),
    val unreadCount: Int = 0,
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val recentNotifications: List<NotificationEntity> = emptyList(),
    val voiceState: VoiceState = VoiceState.IDLE,
    val taskStatus: String = "",
    val lastResponse: String = "",
    val isProcessing: Boolean = false,
    val currentTranscript: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val installedAppDao: InstalledAppDao,
    private val notificationDao: NotificationDao,
    private val reminderDao: ReminderDao,
    private val brainManager: AiBrainManager,
    private val taskPlanner: TaskPlanner,
    private val agentRouter: AgentRouter,
    private val memoryAgent: MemoryAgent,
    val voiceManager: VoiceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<AiMessage>()
    private var currentConversationId: Long = -1L

    init {
        loadProfile()
        loadApps()
        observeNotifications()
        observeVoiceState()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userProfileDao.observeProfile().collect { profile ->
                val name = profile?.name?.ifBlank { "Sir" } ?: "Sir"
                _uiState.update { it.copy(
                    userName = name,
                    greeting = buildGreeting(name)
                )}
                loadBriefing(profile)
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            installedAppDao.observeFavorites().collect { favs ->
                _uiState.update { it.copy(favoriteApps = favs) }
            }
        }
        viewModelScope.launch {
            installedAppDao.observeRecent(8).collect { recent ->
                _uiState.update { it.copy(recentApps = recent) }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationDao.observeUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
        viewModelScope.launch {
            notificationDao.observeRecent().collect { notifs ->
                _uiState.update { it.copy(recentNotifications = notifs.take(5)) }
            }
        }
    }

    private fun observeVoiceState() {
        viewModelScope.launch {
            voiceManager.state.collect { state ->
                _uiState.update { it.copy(voiceState = state) }
            }
        }
        viewModelScope.launch {
            voiceManager.transcript.collect { transcript ->
                _uiState.update { it.copy(currentTranscript = transcript) }
            }
        }
    }

    private suspend fun loadBriefing(profile: UserProfileEntity?) {
        if (profile?.briefingEnabled == false) return
        val upcoming = reminderDao.getUpcoming()
        _uiState.update { it.copy(upcomingReminders = upcoming) }
    }

    fun buildGreeting(name: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning, $name."
            hour < 17 -> "Good afternoon, $name."
            hour < 21 -> "Good evening, $name."
            else -> "Welcome back, $name."
        }
    }

    fun onOrbTapped() {
        if (_uiState.value.voiceState == VoiceState.LISTENING) {
            voiceManager.stopListening()
        } else if (_uiState.value.voiceState == VoiceState.IDLE) {
            startVoiceInput()
        }
    }

    fun startVoiceInput() {
        voiceManager.startListening { transcript ->
            processUserInput(transcript)
        }
    }

    fun processUserInput(input: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, taskStatus = "Processing...") }

            // Store memory snippets passively
            memoryAgent.extractAndStoreFromConversation(input)

            // Plan the task
            val plan = taskPlanner.plan(input)

            // If it's a task (not pure conversation), execute agents
            val agentResults = if (plan.steps.any { it.agentType != com.javis.launcher.ai.AgentType.CONVERSATION }) {
                plan.steps.mapIndexed { idx, step ->
                    _uiState.update { it.copy(taskStatus = step.description) }
                    agentRouter.execute(plan).getOrNull(idx)
                }.filterNotNull()
            } else emptyList()

            // Build context for AI
            val context = buildContext(plan, agentResults)
            conversationHistory.add(AiMessage("user", input))

            val aiResult = brainManager.chat(conversationHistory, context)
            val response = aiResult.getOrElse { "I encountered an issue, but I'm still here to help." }

            conversationHistory.add(AiMessage("assistant", response))
            // Keep context window reasonable
            if (conversationHistory.size > 20) {
                conversationHistory.removeAt(0)
                if (conversationHistory.isNotEmpty()) conversationHistory.removeAt(0)
            }

            _uiState.update { it.copy(
                isProcessing = false,
                taskStatus = "",
                lastResponse = response
            )}

            voiceManager.speak(response)
        }
    }

    private fun buildContext(plan: com.javis.launcher.ai.ExecutionPlan, results: List<com.javis.launcher.agents.AgentResult>): String {
        return if (results.isEmpty()) ""
        else {
            val resultSummary = results.joinToString("\n") { r ->
                if (r.success) "✓ ${r.message}" else "✗ ${r.message}"
            }
            "Task execution results:\n$resultSummary\n\nNarrate what happened concisely and naturally."
        }
    }
}
