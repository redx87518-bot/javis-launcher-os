package com.javis.launcher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── AI Provider Models ──────────────────────────────────────────────────────

enum class AiProvider {
    OPENROUTER, GROQ, DEEPSEEK, TOGETHER, FIREWORKS, OFFLINE
}

data class ChatMessage(
    val role: String, // "user" | "assistant" | "system"
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

data class ChatResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

data class Choice(
    val message: ChatMessage,
    val finish_reason: String = ""
)

data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

// ── ElevenLabs Models ───────────────────────────────────────────────────────

data class TtsRequest(
    val text: String,
    val model_id: String = "eleven_monolingual_v1",
    val voice_settings: VoiceSettings = VoiceSettings()
)

data class VoiceSettings(
    val stability: Float = 0.5f,
    val similarity_boost: Float = 0.75f,
    val style: Float = 0.0f,
    val use_speaker_boost: Boolean = true
)

data class VoiceModel(
    val voice_id: String,
    val name: String,
    val category: String = ""
)

data class VoicesResponse(
    val voices: List<VoiceModel> = emptyList()
)

// ── Room Entities ───────────────────────────────────────────────────────────

@Entity(tableName = "conversation_history")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "",
    val sessionId: String = ""
)

@Entity(tableName = "memory_items")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String, // "profile", "habit", "preference", "routine"
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Int = 0
)

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String = "Other",
    val launchCount: Int = 0,
    val lastLaunched: Long = 0,
    val isFavorite: Boolean = false,
    val iconPath: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val status: String = "pending", // pending | running | completed | failed
    val steps: String = "[]", // JSON array of steps
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val agentType: String = ""
)

@Entity(tableName = "favorite_contacts")
data class FavoriteContactEntity(
    @PrimaryKey val contactId: String,
    val name: String,
    val phoneNumber: String,
    val order: Int = 0
)

@Entity(tableName = "notifications_cache")
data class NotificationCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: Int = 0
)

// ── Domain Models ────────────────────────────────────────────────────────────

data class UserProfile(
    val name: String = "",
    val nickname: String = "Sir",
    val preferredVoice: String = "ElevenLabs",
    val preferredProvider: String = "OPENROUTER",
    val personalityMode: String = "JARVIS"
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val launchCount: Int = 0,
    val isFavorite: Boolean = false
)

data class ContactInfo(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class TaskPlan(
    val intent: String,
    val steps: List<TaskStep>,
    val agentType: String
)

data class TaskStep(
    val index: Int,
    val description: String,
    val action: String,
    val params: Map<String, String> = emptyMap(),
    var status: StepStatus = StepStatus.PENDING
)

enum class StepStatus { PENDING, RUNNING, COMPLETED, FAILED }

data class NotificationSummary(
    val totalUnread: Int,
    val apps: List<AppNotificationGroup>
)

data class AppNotificationGroup(
    val appName: String,
    val packageName: String,
    val count: Int,
    val latestText: String
)

data class JavisState(
    val coreState: CoreState = CoreState.IDLE,
    val currentTask: String = "",
    val isOnline: Boolean = true,
    val currentProvider: AiProvider = AiProvider.OPENROUTER,
    val batteryLevel: Int = 0,
    val unreadNotifications: Int = 0,
    val greeting: String = ""
)

enum class CoreState {
    IDLE, LISTENING, THINKING, SPEAKING, EXECUTING, COMPLETED, ERROR
}
