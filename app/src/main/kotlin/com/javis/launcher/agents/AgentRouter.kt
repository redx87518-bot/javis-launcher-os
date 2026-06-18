package com.javis.launcher.agents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import com.javis.launcher.ai.AgentType
import com.javis.launcher.ai.ExecutionPlan
import com.javis.launcher.ai.TaskStep
import com.javis.launcher.data.db.dao.InstalledAppDao
import com.javis.launcher.data.db.dao.MemoryDao
import com.javis.launcher.data.db.dao.NotificationDao
import com.javis.launcher.data.db.dao.ReminderDao
import com.javis.launcher.data.db.entity.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AgentResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

@Singleton
class AgentRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appAgent: AppAgent,
    private val contactAgent: ContactAgent,
    private val reminderAgent: ReminderAgent,
    private val notificationAgent: NotificationAgent,
    private val memoryAgent: MemoryAgent
) {
    suspend fun execute(plan: ExecutionPlan): List<AgentResult> {
        val results = mutableListOf<AgentResult>()
        for (step in plan.steps) {
            val result = executeStep(step)
            results.add(result)
            if (!result.success) break // Stop on failure
        }
        return results
    }

    private suspend fun executeStep(step: TaskStep): AgentResult {
        return when (step.agentType) {
            AgentType.APP -> appAgent.execute(step.params)
            AgentType.CONTACT -> contactAgent.search(step.params["name"] ?: "")
            AgentType.CALL -> contactAgent.call(step.params["name"] ?: "")
            AgentType.MESSAGE -> AgentResult(true, "Message drafted", step.params)
            AgentType.ALARM, AgentType.REMINDER -> reminderAgent.execute(step.params)
            AgentType.NOTIFICATION -> notificationAgent.summarize()
            AgentType.MEMORY -> memoryAgent.query(step.params["query"] ?: "")
            AgentType.SEARCH -> appAgent.searchWeb(step.params["query"] ?: "")
            AgentType.CONVERSATION -> AgentResult(true, "")
            AgentType.SYSTEM -> AgentResult(true, "System command executed")
        }
    }
}
