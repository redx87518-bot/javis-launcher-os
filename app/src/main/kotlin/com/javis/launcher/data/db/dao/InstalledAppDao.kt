package com.javis.launcher.data.db.dao

import androidx.room.*
import com.javis.launcher.data.db.entity.InstalledAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_app ORDER BY appName ASC")
    fun observeAll(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_app WHERE isFavorite = 1 ORDER BY launchCount DESC")
    fun observeFavorites(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_app ORDER BY lastLaunched DESC LIMIT :limit")
    fun observeRecent(limit: Int = 8): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_app WHERE appName LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' ORDER BY launchCount DESC")
    suspend fun search(query: String): List<InstalledAppEntity>

    @Query("SELECT * FROM installed_app WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): InstalledAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<InstalledAppEntity>)

    @Query("UPDATE installed_app SET launchCount = launchCount + 1, lastLaunched = :now WHERE packageName = :packageName")
    suspend fun recordLaunch(packageName: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE installed_app SET isFavorite = :favorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, favorite: Boolean)

    @Query("DELETE FROM installed_app WHERE packageName NOT IN (:activePackages)")
    suspend fun removeUninstalled(activePackages: List<String>)
}
