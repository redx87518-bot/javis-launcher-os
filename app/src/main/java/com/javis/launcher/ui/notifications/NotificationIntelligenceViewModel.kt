package com.javis.launcher.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.brain.BrainManager
import com.javis.launcher.data.local.NotificationCacheDao
import com.javis.launcher.data.model.NotificationCacheEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationIntelligenceViewModel @Inject constructor(
    private val notificationDao: NotificationCacheDao,
    private val brainManager: BrainManager
) : ViewModel() {

    val notifications = notificationDao.getUnreadNotifications()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun markRead(id: Long) {
        viewModelScope.launch { notificationDao.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationDao.markAllRead() }
    }

    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    fun summarize(grouped: Map<String, List<NotificationCacheEntity>>) {
        if (grouped.isEmpty()) return
        viewModelScope.launch {
            _isSummarizing.value = true
            val bullet = grouped.entries.joinToString("\n") { (app, list) ->
                "- $app: ${list.size} notification(s). Latest: ${list.firstOrNull()?.text ?: list.firstOrNull()?.title ?: ""}"
            }
            val prompt = "You are JAVIS. Summarize these unread notifications concisely and flag the 1-3 most important. Do not invent content.\n$bullet"
            val result = brainManager.chat(prompt)
            _summary.value = result.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "You have ${grouped.values.sumOf { it.size }} unread notifications across ${grouped.size} apps."
            _isSummarizing.value = false
        }
    }
}
