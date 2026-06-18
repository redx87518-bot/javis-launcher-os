package com.javis.launcher.agents

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.javis.launcher.data.db.dao.InstalledAppDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppDao: InstalledAppDao
) {
    suspend fun execute(params: Map<String, String>): AgentResult {
        return when (params["action"]) {
            "launch" -> launchApp(params["query"] ?: "")
            else -> searchApp(params["query"] ?: "")
        }
    }

    suspend fun launchApp(query: String): AgentResult {
        val apps = installedAppDao.search(query)
        val app = apps.firstOrNull() ?: return AgentResult(false, "I couldn't find an app called \"$query\".")

        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
                ?: return AgentResult(false, "Cannot launch ${app.appName}.")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            installedAppDao.recordLaunch(app.packageName)
            AgentResult(true, "Opening ${app.appName}.")
        } catch (e: Exception) {
            AgentResult(false, "Failed to open ${app.appName}: ${e.message}")
        }
    }

    private suspend fun searchApp(query: String): AgentResult {
        val apps = installedAppDao.search(query)
        return if (apps.isNotEmpty()) {
            AgentResult(true, "Found ${apps.size} app(s) matching \"$query\".", mapOf("apps" to apps))
        } else {
            AgentResult(false, "No apps found matching \"$query\".")
        }
    }

    fun searchWeb(query: String): AgentResult {
        return try {
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AgentResult(true, "Searching the web for \"$query\".")
        } catch (e: Exception) {
            AgentResult(false, "Couldn't open browser: ${e.message}")
        }
    }
}
