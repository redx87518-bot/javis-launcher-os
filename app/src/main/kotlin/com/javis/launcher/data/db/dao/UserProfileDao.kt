package com.javis.launcher.data.db.dao

import androidx.room.*
import com.javis.launcher.data.db.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET name = :name, nickname = :nickname WHERE id = 1")
    suspend fun updateName(name: String, nickname: String)

    @Query("UPDATE user_profile SET setupComplete = 1 WHERE id = 1")
    suspend fun markSetupComplete()
}
