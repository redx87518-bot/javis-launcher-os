package com.javis.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JavisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val foregroundChannel = NotificationChannel(
                CHANNEL_FOREGROUND,
                "JAVIS Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "JAVIS AI Assistant running in background"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "JAVIS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important alerts from JAVIS"
            }

            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                "JAVIS Tasks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Task execution notifications"
            }

            manager.createNotificationChannels(
                listOf(foregroundChannel, alertChannel, taskChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_FOREGROUND = "javis_foreground"
        const val CHANNEL_ALERTS = "javis_alerts"
        const val CHANNEL_TASKS = "javis_tasks"
    }
}
