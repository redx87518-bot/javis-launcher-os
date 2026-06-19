package com.javis.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.javis.launcher.R
import com.javis.launcher.data.db.dao.UserProfileDao
import com.javis.launcher.data.preferences.JavisPreferences
import com.javis.launcher.ui.MainActivity
import com.javis.launcher.voice.VoiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class JavisService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var appScanner: AppScannerService
    @Inject lateinit var voiceManager: VoiceManager
    @Inject lateinit var preferences: JavisPreferences
    @Inject lateinit var userProfileDao: UserProfileDao

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SPEAK_GREETING -> serviceScope.launch { speakUnlockGreeting() }
            ACTION_SCAN_APPS -> serviceScope.launch { appScanner.scanInstalledApps() }
            else -> serviceScope.launch { appScanner.scanInstalledApps() }
        }
        return START_STICKY
    }

    private suspend fun speakUnlockGreeting() {
        try {
            val profile = userProfileDao.getProfile() ?: return
            if (!profile.greetingEnabled) return

            val name = profile.name.ifBlank { profile.nickname.ifBlank { "Sir" } }
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 6  -> "Welcome back, $name. It's late — you should rest."
                hour < 12 -> "Good morning, $name. Ready to conquer the day?"
                hour < 17 -> "Good afternoon, $name. How can I assist you?"
                hour < 21 -> "Good evening, $name. Hope your day went well."
                else      -> "Welcome back, $name. Everything is under control."
            }

            if (profile.voiceGreetingEnabled) {
                voiceManager.speak(greeting)
            }
        } catch (_: Exception) {
            // Don't crash service on greeting failure
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_javis_orb)
            .setContentIntent(pi)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_SPEAK_GREETING = "com.javis.launcher.SPEAK_GREETING"
        const val ACTION_SCAN_APPS = "com.javis.launcher.SCAN_APPS"

        fun start(context: Context) {
            val intent = Intent(context, JavisService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, JavisService::class.java))
        }
    }
}
