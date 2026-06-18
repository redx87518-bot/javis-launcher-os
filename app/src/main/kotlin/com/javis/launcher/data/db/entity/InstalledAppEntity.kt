package com.javis.launcher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_app")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String = "other",
    val launchCount: Int = 0,
    val lastLaunched: Long = 0L,
    val isFavorite: Boolean = false,
    val keywords: String = "" // comma-separated search keywords
)
