package com.javis.launcher.agents

import com.javis.launcher.ai.AgentType
import com.javis.launcher.ai.ExecutionPlan
import com.javis.launcher.ai.TaskStep
import javax.inject.Inject
import javax.inject.Singleton

data class AgentResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

@Singleton
class AgentRouter @Inject constructor(
    private val appAgent: AppAgent,
    private val contactAgent: ContactAgent,
    private val reminderAgent: ReminderAgent,
    private val notificationAgent: NotificationAgent,
    private val memoryAgent: MemoryAgent,
    private val messageAgent: MessageAgent
) {
    suspend fun execute(plan: ExecutionPlan): List<AgentResult> {
        val results = mutableListOf<AgentResult>()
        for (step in plan.steps) {
            val result = executeStep(step)
            results.add(result)
            if (!result.success && step.agentType != AgentType.CONVERSATION) break
        }
        return results
    }

    private suspend fun executeStep(step: TaskStep): AgentResult = when (step.agentType) {
        AgentType.APP -> appAgent.execute(step.params)
        AgentType.CONTACT -> contactAgent.search(step.params["name"] ?: "")
        AgentType.CALL -> contactAgent.call(step.params["name"] ?: "")
        AgentType.MESSAGE -> {
            val recipient = step.params["recipient"] ?: ""
            val content = step.params["content"] ?: ""
            val via = step.params["via"] ?: "whatsapp"
            if (via == "sms") messageAgent.draftSms(recipient, content)
            else messageAgent.draftWhatsApp(recipient, content)
        }
        AgentType.ALARM, AgentType.REMINDER -> reminderAgent.execute(step.params)
        AgentType.NOTIFICATION -> notificationAgent.summarize()
        AgentType.MEMORY -> memoryAgent.query(step.params["query"] ?: "")
        AgentType.SEARCH -> appAgent.searchWeb(step.params["query"] ?: "")
        AgentType.CONVERSATION -> AgentResult(true, "")
        AgentType.SYSTEM -> AgentResult(true, "System command processed")
    }
}
