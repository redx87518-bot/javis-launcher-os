package com.javis.launcher.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.javis.launcher.data.model.*

@Database(
    entities = [
        ConversationEntity::class,
        MemoryEntity::class,
        InstalledAppEntity::class,
        TaskEntity::class,
        FavoriteContactEntity::class,
        NotificationCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JavisDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun appDao(): AppDao
    abstract fun taskDao(): TaskDao
    abstract fun contactDao(): FavoriteContactDao
    abstract fun notificationDao(): NotificationCacheDao

    companion object {
        @Volatile private var INSTANCE: JavisDatabase? = null

        fun getInstance(context: Context): JavisDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    JavisDatabase::class.java,
                    "javis_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
