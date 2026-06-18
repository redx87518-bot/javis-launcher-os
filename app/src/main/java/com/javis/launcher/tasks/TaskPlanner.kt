package com.javis.launcher.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import com.javis.launcher.data.local.AppDao
import com.javis.launcher.data.local.TaskDao
import com.javis.launcher.data.model.*
import com.javis.launcher.brain.BrainManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPlanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val brainManager: BrainManager,
    private val appDao: AppDao,
    private val taskDao: TaskDao
) {
    private val _currentTask = MutableStateFlow("")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()

    private val _taskStatus = MutableStateFlow(CoreState.IDLE)
    val taskStatus: StateFlow<CoreState> = _taskStatus.asStateFlow()

    suspend fun processUserInput(input: String): String {
        _taskStatus.value = CoreState.THINKING

        val response = brainManager.chat(input)
        val responseText = response.getOrNull() ?: "I'm sorry, I encountered an issue. Please try again."

        // Parse and execute any actions embedded in the response
        val actionResult = extractAndExecuteAction(responseText)

        _taskStatus.value = if (actionResult != null) CoreState.EXECUTING else CoreState.IDLE

        return if (actionResult != null) {
            val cleanResponse = removeJsonBlock(responseText)
            cleanResponse.ifBlank { actionResult }
        } else {
            responseText
        }
    }

    private suspend fun extractAndExecuteAction(text: String): String? {
        val jsonPattern = Regex("""\{[^}]+\}""")
        val jsonMatch = jsonPattern.find(text) ?: return null

        return try {
            val json = JSONObject(jsonMatch.value)
            val action = json.optString("action")
            val params = json.optJSONObject("params")

            when (action) {
                "OPEN_APP" -> {
                    val query = params?.optString("query") ?: params?.optString("package") ?: ""
                    openApp(query)
                }
                "CALL_CONTACT" -> {
                    val name = params?.optString("name") ?: ""
                    callContact(name)
                }
                "SET_ALARM" -> {
                    val hour = params?.optInt("hour") ?: 7
                    val minute = params?.optInt("minute") ?: 0
                    val message = params?.optString("message") ?: "JAVIS Alarm"
                    setAlarm(hour, minute, message)
                }
                "SEARCH_WEB" -> {
                    val query = params?.optString("query") ?: ""
                    searchWeb(query)
                }
                "SEARCH_CONTACTS" -> {
                    val query = params?.optString("query") ?: ""
                    "Searching contacts for: $query"
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun openApp(query: String): String {
        _currentTask.value = "Opening $query"
        val apps = appDao.searchApps(query)
        val app = apps.firstOrNull()
        if (app != null) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                intent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    appDao.incrementLaunch(app.packageName)
                    _taskStatus.value = CoreState.COMPLETED
                    return "Opening ${app.appName}, Sir."
                }
            } catch (e: Exception) { /* fall through */ }
        }
        return "I couldn't find that app, Sir."
    }

    private fun callContact(name: String): String {
        _currentTask.value = "Calling $name"
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:") 
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return "Searching for $name to call, Sir."
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): String {
        _currentTask.value = "Setting alarm for $hour:$minute"
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _taskStatus.value = CoreState.COMPLETED
            return "Alarm set for ${formatTime(hour, minute)}, Sir."
        } catch (e: Exception) {
            return "I couldn't set the alarm. Please check alarm permissions, Sir."
        }
    }

    private fun searchWeb(query: String): String {
        _currentTask.value = "Searching: $query"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Searching the web for: $query, Sir."
    }

    private fun removeJsonBlock(text: String): String =
        text.replace(Regex("""\{[^}]+\}\s*"""), "").trim()

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val m = minute.toString().padStart(2, '0')
        return "$h:$m $period"
    }

    fun resetState() {
        _currentTask.value = ""
        _taskStatus.value = CoreState.IDLE
    }
}
