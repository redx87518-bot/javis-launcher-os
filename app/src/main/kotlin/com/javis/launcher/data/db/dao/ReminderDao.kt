package com.javis.launcher.data.db.dao

import androidx.room.*
import com.javis.launcher.data.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder WHERE isCompleted = 0 AND isCancelled = 0 ORDER BY triggerAt ASC")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminder WHERE triggerAt > :now AND isCompleted = 0 AND isCancelled = 0 ORDER BY triggerAt ASC LIMIT 5")
    suspend fun getUpcoming(now: Long = System.currentTimeMillis()): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("UPDATE reminder SET isCompleted = 1 WHERE id = :id")
    suspend fun complete(id: Long)

    @Query("UPDATE reminder SET isCancelled = 1 WHERE id = :id")
    suspend fun cancel(id: Long)

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}
