package com.javis.launcher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val prefs = context.getSharedPreferences("javis_prefs", Context.MODE_PRIVATE)
        val dailyBriefingEnabled = prefs.getBoolean("daily_briefing_enabled", true)
        if (!dailyBriefingEnabled) return

        val frequency = prefs.getString("briefing_frequency", "FIRST_UNLOCK") ?: "FIRST_UNLOCK"
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastBriefingDate = prefs.getString("last_briefing_date", "") ?: ""

        val shouldBrief = when (frequency) {
            "EVERY_UNLOCK" -> true
            "FIRST_UNLOCK" -> today != lastBriefingDate
            "EVERY_HOUR" -> {
                val lastTime = prefs.getLong("last_briefing_time", 0L)
                System.currentTimeMillis() - lastTime > 60 * 60 * 1000L
            }
            else -> false
        }

        if (shouldBrief) {
            prefs.edit()
                .putString("last_briefing_date", today)
                .putLong("last_briefing_time", System.currentTimeMillis())
                .apply()
            val broadcastIntent = Intent("com.javis.launcher.DAILY_BRIEFING")
                .setPackage("com.javis.launcher")
            context.sendBroadcast(broadcastIntent)
        }
    }
}
