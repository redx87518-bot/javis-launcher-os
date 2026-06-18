package com.javis.launcher.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.javis.launcher.data.db.dao.InstalledAppDao
import com.javis.launcher.data.db.entity.InstalledAppEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScannerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppDao: InstalledAppDao
) {
    suspend fun scanInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val activities = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val apps = activities.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            val name = info.loadLabel(pm).toString()
            InstalledAppEntity(
                packageName = pkg,
                appName = name,
                category = inferCategory(pkg, name),
                keywords = buildKeywords(pkg, name)
            )
        }

        val activePackages = apps.map { it.packageName }
        installedAppDao.upsertAll(apps)
        if (activePackages.isNotEmpty()) {
            installedAppDao.removeUninstalled(activePackages)
        }
    }

    private fun inferCategory(pkg: String, name: String): String {
        val lower = "$pkg $name".lowercase()
        return when {
            lower.contains("social") || lower.contains("whatsapp") || lower.contains("facebook") ||
            lower.contains("telegram") || lower.contains("twitter") || lower.contains("instagram") -> "social"
            lower.contains("video") || lower.contains("youtube") || lower.contains("netflix") ||
            lower.contains("tiktok") || lower.contains("spotify") || lower.contains("music") -> "entertainment"
            lower.contains("map") || lower.contains("navigation") || lower.contains("uber") ||
            lower.contains("location") -> "navigation"
            lower.contains("camera") || lower.contains("photo") || lower.contains("gallery") -> "media"
            lower.contains("game") || lower.contains("play") -> "games"
            lower.contains("bank") || lower.contains("finance") || lower.contains("pay") ||
            lower.contains("wallet") -> "finance"
            lower.contains("news") || lower.contains("read") || lower.contains("article") -> "news"
            lower.contains("shop") || lower.contains("store") || lower.contains("order") -> "shopping"
            lower.contains("health") || lower.contains("fitness") || lower.contains("medical") -> "health"
            lower.contains("productivity") || lower.contains("calendar") || lower.contains("note") ||
            lower.contains("todo") || lower.contains("task") -> "productivity"
            lower.contains("browser") || lower.contains("chrome") || lower.contains("firefox") -> "browser"
            lower.contains("setting") || lower.contains("system") -> "system"
            else -> "other"
        }
    }

    private fun buildKeywords(pkg: String, name: String): String {
        val words = mutableSetOf<String>()
        words.addAll(name.lowercase().split(Regex("\\s+")))
        words.addAll(pkg.split(".").filter { it.length > 2 })
        return words.joinToString(",")
    }
}
