package com.javis.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.javis.launcher.data.db.dao.NotificationDao
import com.javis.launcher.data.db.entity.NotificationEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JavisNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var notificationDao: NotificationDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val ignoredPackages = setOf(
        "com.javis.launcher", "com.android.systemui", "android",
        "com.android.launcher3", "com.google.android.googlequicksearchbox"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg in ignoredPackages) return
        if (sbn.isOngoing) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }

        scope.launch {
            notificationDao.insert(
                NotificationEntity(
                    packageName = pkg,
                    appName = appName,
                    title = title,
                    text = text
                )
            )
            // Cleanup old notifications (keep last 7 days)
            notificationDao.deleteOlderThan(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Mark as read when dismissed
    }
}
