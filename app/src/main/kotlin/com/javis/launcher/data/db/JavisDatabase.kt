package com.javis.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.javis.launcher.data.db.converters.Converters
import com.javis.launcher.data.db.dao.*
import com.javis.launcher.data.db.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        MemoryEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        InstalledAppEntity::class,
        ReminderEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JavisDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun memoryDao(): MemoryDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "javis_database"
    }
}
