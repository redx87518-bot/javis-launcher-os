package com.javis.launcher.agents

import com.javis.launcher.data.db.dao.MemoryDao
import com.javis.launcher.data.db.entity.MemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryAgent @Inject constructor(
    private val memoryDao: MemoryDao
) {
    suspend fun query(query: String): AgentResult {
        val results = memoryDao.search(query)
        return if (results.isNotEmpty()) {
            val facts = results.joinToString("\n") { "• ${it.key}: ${it.value}" }
            AgentResult(true, facts, mapOf("memories" to results))
        } else {
            AgentResult(false, "I don't have any memories about that yet.")
        }
    }

    suspend fun store(key: String, value: String, category: String = "general") {
        memoryDao.upsert(
            MemoryEntity(key = key, value = value, category = category)
        )
    }

    suspend fun extractAndStoreFromConversation(userMessage: String) {
        val lower = userMessage.lowercase()
        when {
            lower.contains("my name is") -> {
                val name = lower.substringAfter("my name is").trim()
                    .split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: return
                store("user_name", name, "personal")
            }
            lower.contains("call me") -> {
                val nickname = lower.substringAfter("call me").trim()
                    .split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: return
                store("user_nickname", nickname, "personal")
            }
            lower.contains("i like") || lower.contains("i love") || lower.contains("i enjoy") -> {
                val preference = lower
                    .replace("i like", "").replace("i love", "").replace("i enjoy", "").trim()
                if (preference.isNotBlank()) store("interest_${System.currentTimeMillis()}", preference, "interest")
            }
            lower.contains("i hate") || lower.contains("i don't like") || lower.contains("i dislike") -> {
                val dislike = lower
                    .replace("i hate", "").replace("i don't like", "").replace("i dislike", "").trim()
                if (dislike.isNotBlank()) store("dislike_${System.currentTimeMillis()}", dislike, "preference")
            }
        }
    }
}
