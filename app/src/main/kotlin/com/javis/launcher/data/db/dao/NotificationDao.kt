package com.javis.launcher.data.db.dao

import androidx.room.*
import com.javis.launcher.data.db.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification ORDER BY timestamp DESC LIMIT 50")
    fun observeRecent(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notification WHERE isRead = 0 ORDER BY timestamp DESC")
    fun observeUnread(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notification WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Query("UPDATE notification SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notification SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notification WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
