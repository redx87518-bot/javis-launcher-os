package com.javis.launcher.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
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
        val responseText = response.getOrNull()
            ?: "I'm having trouble connecting right now. Please try again."

        val cleanText = executeActionsAndClean(responseText)

        _taskStatus.value = CoreState.IDLE
        return cleanText
    }

    // ── JSON extraction that handles nested braces ────────────────────────────

    private fun extractFirstJson(text: String): String? {
        var depth = 0
        var startIdx = -1
        for (i in text.indices) {
            when (text[i]) {
                '{' -> {
                    if (depth == 0) startIdx = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && startIdx != -1) {
                        return text.substring(startIdx, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun removeAllJsonBlocks(text: String): String {
        val sb = StringBuilder()
        var depth = 0
        var inJson = false
        for (ch in text) {
            when {
                ch == '{' -> { inJson = true; depth++ }
                ch == '}' && inJson -> {
                    depth--
                    if (depth == 0) inJson = false
                }
                !inJson -> sb.append(ch)
            }
        }
        return sb.toString().trim().replace(Regex("\\s{2,}"), " ")
    }

    private fun executeActionsAndClean(text: String): String {
        var remaining = text
        var executed = false

        // Execute ALL action blocks found in the response
        while (true) {
            val jsonStr = extractFirstJson(remaining) ?: break
            try {
                val json = JSONObject(jsonStr)
                val action = json.optString("action", "")
                if (action.isBlank()) break
                val params = json.optJSONObject("params")
                executeAction(action, params)
                executed = true
                _taskStatus.value = CoreState.EXECUTING
            } catch (_: Exception) {}
            // Remove this JSON block and search for next
            val idx = remaining.indexOf(jsonStr)
            if (idx < 0) break
            remaining = remaining.removeRange(idx, idx + jsonStr.length)
        }

        return removeAllJsonBlocks(text).ifBlank {
            if (executed) "Done." else text
        }
    }

    private fun executeAction(action: String, params: JSONObject?) {
        when (action.uppercase()) {
            "OPEN_APP" -> {
                val pkg = params?.optString("package") ?: ""
                val query = params?.optString("query")
                    ?: params?.optString("name")
                    ?: params?.optString("app")
                    ?: ""
                launchApp(packageName = pkg, query = query)
            }
            "CALL_CONTACT", "CALL" -> {
                val phone = params?.optString("phone")
                    ?: params?.optString("number")
                    ?: params?.optString("phoneNumber")
                    ?: ""
                val name = params?.optString("name") ?: params?.optString("contact") ?: ""
                callNumber(phone = phone, name = name)
            }
            "SET_ALARM", "ALARM" -> {
                val (hour, minute) = parseTime(params)
                val message = params?.optString("message")
                    ?: params?.optString("label")
                    ?: "JAVIS Alarm"
                val repeat = params?.optBoolean("repeat") ?: false
                setAlarm(hour, minute, message, repeat)
            }
            "SET_TIMER", "TIMER" -> {
                val seconds = (params?.optInt("seconds") ?: 0)
                    + (params?.optInt("minutes") ?: 0) * 60
                    + (params?.optInt("hours") ?: 0) * 3600
                setTimer(seconds.coerceAtLeast(60))
            }
            "SEARCH_WEB", "WEB_SEARCH", "GOOGLE" -> {
                val query = params?.optString("query") ?: ""
                searchWeb(query)
            }
            "SEND_SMS", "SMS" -> {
                val phone = params?.optString("phone") ?: params?.optString("number") ?: ""
                val message = params?.optString("message") ?: params?.optString("text") ?: ""
                sendSms(phone, message)
            }
            "OPEN_WHATSAPP", "WHATSAPP" -> {
                val phone = params?.optString("phone") ?: params?.optString("number") ?: ""
                val message = params?.optString("message") ?: params?.optString("text") ?: ""
                openWhatsApp(phone, message)
            }
            "OPEN_SETTINGS", "SETTINGS" -> {
                openSettings(params?.optString("page") ?: "")
            }
            "PLAY_MUSIC", "MUSIC" -> {
                val query = params?.optString("query") ?: params?.optString("song") ?: ""
                playMusic(query)
            }
            "SET_REMINDER", "REMINDER" -> {
                val (hour, minute) = parseTime(params)
                val message = params?.optString("message") ?: params?.optString("text") ?: "Reminder"
                setAlarm(hour, minute, message, false)
            }
            else -> {
                // Try generic app launch by action name as query
                val query = params?.optString("query")
                    ?: params?.optString("app")
                    ?: action.lowercase().replace("_", " ")
                launchApp(packageName = "", query = query)
            }
        }
    }

    // ── Action implementations ────────────────────────────────────────────────

    private fun launchApp(packageName: String, query: String) {
        _currentTask.value = "Opening app..."
        val pm = context.packageManager

        // 1. Try exact package name first (most reliable)
        if (packageName.isNotBlank() && packageName.contains(".")) {
            try {
                val intent = pm.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    _taskStatus.value = CoreState.COMPLETED
                    return
                }
            } catch (_: Exception) {}
        }

        // 2. Search installed apps by query (name match)
        if (query.isNotBlank()) {
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val allApps = pm.queryIntentActivities(mainIntent, 0)
                val queryLower = query.lowercase().trim()

                // Exact name match first
                val exact = allApps.firstOrNull { ri ->
                    pm.getApplicationLabel(ri.activityInfo.applicationInfo)
                        .toString().lowercase() == queryLower
                }
                // Then partial match
                val partial = allApps.firstOrNull { ri ->
                    pm.getApplicationLabel(ri.activityInfo.applicationInfo)
                        .toString().lowercase().contains(queryLower)
                }

                val match = exact ?: partial
                if (match != null) {
                    val pkg = match.activityInfo.packageName
                    val intent = pm.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        _taskStatus.value = CoreState.COMPLETED
                        return
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Try DB search
        _taskStatus.value = CoreState.COMPLETED
    }

    private fun callNumber(phone: String, name: String) {
        _currentTask.value = "Calling $name"
        try {
            if (phone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${phone.replace(" ", "")}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else if (name.isNotBlank()) {
                // Open dialer with contact search
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String, repeat: Boolean) {
        _currentTask.value = "Setting alarm ${formatTime(hour, minute)}"
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                if (repeat) {
                    putExtra(AlarmClock.EXTRA_DAYS, arrayListOf(2, 3, 4, 5, 6, 7, 1))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setTimer(seconds: Int) {
        _currentTask.value = "Setting timer"
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchWeb(query: String) {
        _currentTask.value = "Searching: $query"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendSms(phone: String, message: String) {
        _currentTask.value = "Sending SMS"
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openWhatsApp(phone: String, message: String) {
        _currentTask.value = "Opening WhatsApp"
        try {
            if (phone.isNotBlank()) {
                val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // Just open WhatsApp
                val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) context.startActivity(intent)
            }
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            // Fallback: open WhatsApp from Play Store
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun openSettings(page: String) {
        _currentTask.value = "Opening settings"
        try {
            val action = when (page.lowercase()) {
                "wifi", "network" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                "notification" -> Settings.ACTION_NOTIFICATION_SETTINGS
                "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playMusic(query: String) {
        _currentTask.value = "Playing music"
        try {
            if (query.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://open.spotify.com/search/$query")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // Try to open music app
                for (pkg in listOf("com.spotify.music", "com.google.android.music",
                    "com.apple.android.music", "com.soundcloud.android")) {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        break
                    }
                }
            }
            _taskStatus.value = CoreState.COMPLETED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseTime(params: JSONObject?): Pair<Int, Int> {
        // Handle "time":"20:00" format from AI
        val timeStr = params?.optString("time") ?: ""
        if (timeStr.contains(":")) {
            val parts = timeStr.split(":")
            val h = parts[0].trim().toIntOrNull() ?: 7
            val m = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0
            return Pair(h, m)
        }
        // Handle "hour":20 "minute":0 format
        val hour = params?.optInt("hour") ?: 7
        val minute = params?.optInt("minute") ?: 0
        return Pair(hour, minute)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$h:${minute.toString().padStart(2, '0')} $period"
    }

    fun resetState() {
        _currentTask.value = ""
        _taskStatus.value = CoreState.IDLE
    }
}
