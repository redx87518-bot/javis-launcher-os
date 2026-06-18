package com.javis.launcher.services

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.javis.launcher.data.local.NotificationCacheDao
import com.javis.launcher.data.model.NotificationCacheEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class JavisNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationDao: NotificationCacheDao
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val packageName = sbn.packageName

        // Skip JAVIS own notifications
        if (packageName == "com.javis.launcher") return
        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ).toString()
        } catch (e: Exception) { packageName }

        scope.launch {
            try {
                notificationDao.insert(
                    NotificationCacheEntity(
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        priority = notification.priority
                    )
                )
                // Clean old notifications (keep last 100)
                notificationDao.deleteOlderThan(
                    System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
