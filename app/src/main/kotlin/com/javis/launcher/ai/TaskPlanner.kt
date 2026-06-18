package com.javis.launcher.ai

import javax.inject.Inject
import javax.inject.Singleton

enum class TaskIntent {
    OPEN_APP, CALL_CONTACT, SEND_MESSAGE, SET_ALARM, SET_REMINDER, SET_TIMER,
    SEARCH_WEB, SEARCH_APP, READ_NOTIFICATIONS, CONVERSATION, MEMORY_QUERY,
    MEMORY_STORE, VOLUME_CONTROL, BRIGHTNESS_CONTROL, UNKNOWN
}

data class TaskStep(
    val description: String,
    val agentType: AgentType,
    val params: Map<String, String> = emptyMap()
)

data class ExecutionPlan(
    val intent: TaskIntent,
    val userMessage: String,
    val steps: List<TaskStep>,
    val requiresConfirmation: Boolean = false,
    val confirmationMessage: String = ""
)

enum class AgentType {
    APP, CONTACT, CALL, MESSAGE, ALARM, REMINDER, NOTIFICATION,
    SEARCH, MEMORY, CONVERSATION, SYSTEM
}

@Singleton
class TaskPlanner @Inject constructor() {

    fun plan(userMessage: String): ExecutionPlan {
        val lower = userMessage.lowercase().trim()

        return when {
            // Open/launch app
            lower.matches(Regex(".*(open|launch|start|run|go to)\\s+(.+)")) -> {
                val appName = extractTarget(lower, listOf("open", "launch", "start", "run", "go to"))
                ExecutionPlan(
                    intent = TaskIntent.OPEN_APP,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Searching for $appName", AgentType.APP, mapOf("query" to appName)),
                        TaskStep("Opening $appName", AgentType.APP, mapOf("action" to "launch", "query" to appName))
                    )
                )
            }

            // Call contact
            lower.matches(Regex(".*(call|phone|dial|ring)\\s+(.+)")) -> {
                val name = extractTarget(lower, listOf("call", "phone", "dial", "ring"))
                ExecutionPlan(
                    intent = TaskIntent.CALL_CONTACT,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Finding contact: $name", AgentType.CONTACT, mapOf("name" to name)),
                        TaskStep("Calling $name", AgentType.CALL, mapOf("name" to name))
                    ),
                    requiresConfirmation = false // Call directly per user preference
                )
            }

            // Send message
            lower.matches(Regex(".*(message|text|whatsapp|sms|send).*(to\\s+)?(.+)")) -> {
                val name = extractMessageTarget(lower)
                val content = extractMessageContent(lower)
                ExecutionPlan(
                    intent = TaskIntent.SEND_MESSAGE,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Drafting message to $name", AgentType.MESSAGE, mapOf("recipient" to name, "content" to content))
                    ),
                    requiresConfirmation = true,
                    confirmationMessage = "I prepared the message: \"$content\". Should I send it?"
                )
            }

            // Set alarm
            lower.matches(Regex(".*(alarm|wake me|wake up).*(at|for)?.*\\d.*")) -> {
                val time = extractTime(lower)
                ExecutionPlan(
                    intent = TaskIntent.SET_ALARM,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Setting alarm for $time", AgentType.ALARM, mapOf("time" to time, "type" to "alarm"))
                    )
                )
            }

            // Set reminder
            lower.matches(Regex(".*(remind|reminder).+")) -> {
                val content = lower.removePrefix("remind me to ").removePrefix("remind me ")
                    .removePrefix("set a reminder ").removePrefix("create reminder ")
                ExecutionPlan(
                    intent = TaskIntent.SET_REMINDER,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Creating reminder: $content", AgentType.REMINDER, mapOf("content" to content))
                    )
                )
            }

            // Set timer
            lower.matches(Regex(".*(timer|countdown).+(\\d+).*(minute|second|hour).*")) -> {
                val duration = extractDuration(lower)
                ExecutionPlan(
                    intent = TaskIntent.SET_TIMER,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Setting timer for $duration", AgentType.ALARM, mapOf("type" to "timer", "duration" to duration))
                    )
                )
            }

            // Read notifications
            lower.matches(Regex(".*(notification|miss|what.*(happen|new)|inbox|update).*")) -> {
                ExecutionPlan(
                    intent = TaskIntent.READ_NOTIFICATIONS,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Reading notifications", AgentType.NOTIFICATION, emptyMap())
                    )
                )
            }

            // Memory query
            lower.matches(Regex(".*(remember|my name|who am i|what.*(i|my)|recall|know about me).*")) -> {
                ExecutionPlan(
                    intent = TaskIntent.MEMORY_QUERY,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Searching memory", AgentType.MEMORY, mapOf("query" to userMessage))
                    )
                )
            }

            // Search
            lower.matches(Regex(".*(search|find|look up|google|browse)\\s+(.+)")) -> {
                val query = extractTarget(lower, listOf("search", "find", "look up", "google", "browse", "search for"))
                ExecutionPlan(
                    intent = TaskIntent.SEARCH_WEB,
                    userMessage = userMessage,
                    steps = listOf(
                        TaskStep("Searching for: $query", AgentType.SEARCH, mapOf("query" to query))
                    )
                )
            }

            // Default: conversation
            else -> ExecutionPlan(
                intent = TaskIntent.CONVERSATION,
                userMessage = userMessage,
                steps = listOf(
                    TaskStep("Processing your message", AgentType.CONVERSATION, emptyMap())
                )
            )
        }
    }

    private fun extractTarget(msg: String, prefixes: List<String>): String {
        var result = msg
        for (prefix in prefixes) {
            result = result.removePrefix(prefix).trim()
        }
        return result.takeIf { it.isNotBlank() } ?: msg
    }

    private fun extractMessageTarget(msg: String): String {
        val patterns = listOf("message to ", "text to ", "send to ", "whatsapp to ", "message ")
        for (p in patterns) {
            val idx = msg.indexOf(p)
            if (idx >= 0) {
                val after = msg.substring(idx + p.length)
                return after.split(Regex("\\s+(saying|that|with|:)")).firstOrNull()?.trim() ?: after.trim()
            }
        }
        return "contact"
    }

    private fun extractMessageContent(msg: String): String {
        val patterns = listOf("saying ", "that ", "message: ", ": ")
        for (p in patterns) {
            val idx = msg.indexOf(p)
            if (idx >= 0) return msg.substring(idx + p.length).trim()
        }
        return ""
    }

    private fun extractTime(msg: String): String {
        val timePattern = Regex("(\\d{1,2})(:\\d{2})?\\s*(am|pm)?")
        return timePattern.find(msg)?.value?.trim() ?: "scheduled time"
    }

    private fun extractDuration(msg: String): String {
        val pattern = Regex("(\\d+)\\s*(minute|second|hour|min|sec|hr)")
        return pattern.find(msg)?.value?.trim() ?: "set duration"
    }
}
