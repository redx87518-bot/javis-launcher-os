package com.javis.launcher.data.local

import androidx.room.*
import com.javis.launcher.data.model.*
import kotlinx.coroutines.flow.Flow

typealias InstalledAppEntity = com.javis.launcher.data.model.InstalledAppEntity

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSessionHistory(sessionId: String): List<ConversationEntity>

    @Insert
    suspend fun insert(message: ConversationEntity): Long

    @Query("DELETE FROM conversation_history WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM conversation_history")
    suspend fun getCount(): Int
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY priority DESC, timestamp DESC")
    fun getAllMemory(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memory_items WHERE category = :category")
    suspend fun getByCategory(category: String): List<MemoryEntity>

    @Query("SELECT * FROM memory_items WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: MemoryEntity): Long

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AppDao {
    @Query("SELECT * FROM installed_apps ORDER BY launchCount DESC")
    fun getAllApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE isFavorite = 1 ORDER BY launchCount DESC LIMIT 8")
    fun getFavoriteApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps ORDER BY lastLaunched DESC LIMIT 8")
    fun getRecentApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE appName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchApps(query: String): List<InstalledAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<InstalledAppEntity>)

    @Query("UPDATE installed_apps SET launchCount = launchCount + 1, lastLaunched = :timestamp WHERE packageName = :packageName")
    suspend fun incrementLaunch(packageName: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE installed_apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC LIMIT 20")
    fun getRecentTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'running' OR status = 'pending' ORDER BY createdAt ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedAt: Long? = null)
}

@Dao
interface FavoriteContactDao {
    @Query("SELECT * FROM favorite_contacts ORDER BY `order` ASC")
    fun getFavoriteContacts(): Flow<List<FavoriteContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: FavoriteContactEntity)

    @Query("DELETE FROM favorite_contacts WHERE contactId = :id")
    suspend fun delete(id: String)
}

@Dao
interface NotificationCacheDao {
    @Query("SELECT * FROM notifications_cache WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationCacheEntity>>

    @Query("SELECT COUNT(*) FROM notifications_cache WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(notification: NotificationCacheEntity): Long

    @Query("UPDATE notifications_cache SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications_cache SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications_cache WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
