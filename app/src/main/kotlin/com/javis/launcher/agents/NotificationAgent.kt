package com.javis.launcher.agents

import com.javis.launcher.data.db.dao.NotificationDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAgent @Inject constructor(
    private val notificationDao: NotificationDao
) {
    suspend fun summarize(): AgentResult {
        val unread = mutableListOf<com.javis.launcher.data.db.entity.NotificationEntity>()
        notificationDao.observeUnread().collect { list ->
            unread.addAll(list)
            return@collect
        }
        if (unread.isEmpty()) return AgentResult(true, "No new notifications.")

        val grouped = unread.groupBy { it.appName }
        val summary = StringBuilder()
        grouped.forEach { (app, notifs) ->
            summary.appendLine("$app: ${notifs.size} notification${if (notifs.size > 1) "s" else ""}")
        }
        return AgentResult(true, "You have ${unread.size} notification(s):\n${summary.toString().trim()}")
    }
}
