package com.javis.launcher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationPermissionUtils {

    /** True when JAVIS is enabled as a Notification Listener. */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, "com.javis.launcher.services.JavisNotificationListenerService")
        return NotificationManagerCompat.getNotificationListeners(context)
            ?.any { it.flattenToString() == cn.flattenToString() } == true
    }

    /** Opens the system screen where the user enables the notification listener. */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }
}
