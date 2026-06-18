package com.javis.launcher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val nickname: String = "",
    val preferredVoiceId: String = "",
    val preferredAiProvider: String = "openrouter",
    val greetingEnabled: Boolean = true,
    val voiceGreetingEnabled: Boolean = true,
    val briefingEnabled: Boolean = true,
    val greetingFrequency: String = "every_unlock",
    val setupComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
