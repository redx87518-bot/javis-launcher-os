package com.javis.launcher.data.db.dao

import androidx.room.*
import com.javis.launcher.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatest(): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity): Long

    @Query("UPDATE conversation SET updatedAt = :now, messageCount = messageCount + 1 WHERE id = :id")
    suspend fun incrementMessages(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(conversation: ConversationEntity)
}
