package com.javis.launcher.ui.appdrawer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.data.local.AppDao
import com.javis.launcher.data.model.AppInfo
import com.javis.launcher.data.model.InstalledAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) : ViewModel() {

    val allApps = appDao.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps = appDao.getFavoriteApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentApps = appDao.getRecentApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) context.startActivity(intent)
                appDao.incrementLaunch(packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(packageName: String, current: Boolean) {
        viewModelScope.launch { appDao.setFavorite(packageName, !current) }
    }

    /**
     * Natural-language aware app search (spec #8, #25).
     * Combines category keywords, fuzzy name matching and simple intent phrasing
     * so the user does not need the exact app name. Online semantic search is
     * delegated to the conversation brain elsewhere; this stays local-first.
     */
    fun search(query: String, apps: List<InstalledAppEntity>): List<AppInfo> {
        val q = query.lowercase().trim()
        if (q.isBlank()) return emptyList()

        val categoryHints = mapOf(
            "message" to listOf("Social", "Messaging"),
            "chat" to listOf("Social", "Messaging"),
            "social" to listOf("Social"),
            "browser" to listOf("Browser"),
            "web" to listOf("Browser"),
            "email" to listOf("Email"),
            "mail" to listOf("Email"),
            "music" to listOf("Music"),
            "song" to listOf("Music"),
            "video" to listOf("Video"),
            "movie" to listOf("Video"),
            "photo" to listOf("Photos"),
            "gallery" to listOf("Photos"),
            "camera" to listOf("Camera"),
            "map" to listOf("Navigation"),
            "nav" to listOf("Navigation"),
            "game" to listOf("Games"),
            "study" to listOf("Productivity"),
            "learn" to listOf("Productivity"),
            "productivity" to listOf("Productivity"),
            "pdf" to listOf("Productivity"),
            "document" to listOf("Productivity"),
            "security" to listOf("Developer"),
            "cyber" to listOf("Developer"),
            "code" to listOf("Developer"),
            "bank" to listOf("Finance"),
            "call" to listOf("Communication"),
            "phone" to listOf("Communication")
        )

        val scored = apps.map { app ->
            val name = app.appName.lowercase()
            val cat = app.category.lowercase()
            var score = 0

            if (name == q) score += 100
            if (name.contains(q)) score += 60
            if (q.contains(name) && name.length > 2) score += 40
            val words = name.split(" ", ".", "-", "_")
            if (words.any { it == q || (q.length >= 4 && it.contains(q)) }) score += 50

            categoryHints.forEach { (keyword, cats) ->
                if (q.contains(keyword)) {
                    if (cat in cats.map { it.lowercase() }) score += 45
                    if (name.contains(keyword)) score += 30
                }
            }
            if (cat == q) score += 25
            score to app
        }.filter { it.first > 0 }

        return scored.sortedByDescending { it.first }
            .map { AppInfo(it.second.packageName, it.second.appName, it.second.category, it.second.launchCount, it.second.isFavorite) }
    }

    fun categories(apps: List<InstalledAppEntity>): List<Pair<String, List<InstalledAppEntity>>> {
        return apps.groupBy { it.category.ifBlank { "Other" } }
            .toList()
            .sortedBy { it.first }
    }
}
