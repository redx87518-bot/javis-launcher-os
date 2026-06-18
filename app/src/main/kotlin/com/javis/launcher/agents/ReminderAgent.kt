package com.javis.launcher.agents

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.javis.launcher.data.db.dao.ReminderDao
import com.javis.launcher.data.db.entity.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) {
    suspend fun execute(params: Map<String, String>): AgentResult {
        return when (params["type"]) {
            "alarm" -> setAlarm(params)
            "timer" -> setTimer(params)
            else -> setReminder(params)
        }
    }

    private suspend fun setAlarm(params: Map<String, String>): AgentResult {
        val timeStr = params["time"] ?: return AgentResult(false, "Please specify an alarm time.")
        return try {
            val (hour, minute) = parseTime(timeStr)
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JAVIS Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            reminderDao.insert(
                ReminderEntity(
                    title = "Alarm at $timeStr",
                    triggerAt = calculateTriggerTime(hour, minute),
                    type = "alarm"
                )
            )
            AgentResult(true, "Alarm set for $timeStr.")
        } catch (e: Exception) {
            AgentResult(false, "Couldn't set alarm: ${e.message}")
        }
    }

    private suspend fun setTimer(params: Map<String, String>): AgentResult {
        val durationStr = params["duration"] ?: "5 minutes"
        val seconds = parseDurationToSeconds(durationStr)
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JAVIS Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AgentResult(true, "Timer set for $durationStr.")
        } catch (e: Exception) {
            AgentResult(false, "Couldn't set timer: ${e.message}")
        }
    }

    private suspend fun setReminder(params: Map<String, String>): AgentResult {
        val content = params["content"] ?: return AgentResult(false, "What should I remind you about?")
        val triggerAt = System.currentTimeMillis() + (60 * 60 * 1000L) // default 1 hour
        reminderDao.insert(
            ReminderEntity(
                title = content,
                triggerAt = triggerAt,
                type = "reminder"
            )
        )
        return AgentResult(true, "Reminder set: $content")
    }

    private fun parseTime(timeStr: String): Pair<Int, Int> {
        val cleaned = timeStr.trim().lowercase()
        val isPM = cleaned.contains("pm")
        val isAM = cleaned.contains("am")
        val numbers = cleaned.replace(Regex("[^0-9:]"), "")
        val parts = numbers.split(":")
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (isPM && hour < 12) hour += 12
        if (isAM && hour == 12) hour = 0
        return Pair(hour, minute)
    }

    private fun calculateTriggerTime(hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val trigger = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }
        if (trigger.before(now)) trigger.add(java.util.Calendar.DAY_OF_MONTH, 1)
        return trigger.timeInMillis
    }

    private fun parseDurationToSeconds(duration: String): Int {
        val pattern = Regex("(\\d+)\\s*(minute|second|hour|min|sec|hr)")
        val match = pattern.find(duration.lowercase()) ?: return 300
        val value = match.groupValues[1].toIntOrNull() ?: 5
        return when {
            match.groupValues[2].startsWith("h") -> value * 3600
            match.groupValues[2].startsWith("m") -> value * 60
            else -> value
        }
    }
}
