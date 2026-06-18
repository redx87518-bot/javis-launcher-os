package com.javis.launcher.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.ai.AiBrainManager
import com.javis.launcher.ai.AiProvider
import com.javis.launcher.data.db.dao.NotificationDao
import com.javis.launcher.data.db.dao.ReminderDao
import com.javis.launcher.data.db.entity.NotificationEntity
import com.javis.launcher.data.db.entity.ReminderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val unreadNotifs: Int = 0,
    val notifications: List<NotificationEntity> = emptyList(),
    val upcomingReminders: List<ReminderEntity> = emptyList(),
    val providerName: String = "OpenRouter",
    val isOnline: Boolean = true,
    val lastResponseMs: Long = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    private val reminderDao: ReminderDao,
    private val brainManager: AiBrainManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationDao.observeUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadNotifs = count) }
            }
        }
        viewModelScope.launch {
            notificationDao.observeRecent().collect { notifs ->
                _uiState.update { it.copy(notifications = notifs.take(20)) }
            }
        }
        viewModelScope.launch {
            reminderDao.observeActive().collect { reminders ->
                _uiState.update { it.copy(upcomingReminders = reminders.take(5)) }
            }
        }
        viewModelScope.launch {
            brainManager.state.collect { s ->
                _uiState.update { it.copy(
                    providerName = s.currentProvider.displayName,
                    isOnline = s.currentProvider != AiProvider.OFFLINE,
                    lastResponseMs = s.lastResponseMs
                )}
            }
        }
    }

    fun completeReminder(id: Long) {
        viewModelScope.launch { reminderDao.complete(id) }
    }
}
