package com.javis.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.javis.launcher.data.preferences.JavisPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: JavisPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        scope.launch {
            // Record the unlock timestamp
            preferences.recordUnlock()

            // Start (or signal) the foreground service to speak the greeting
            val serviceIntent = Intent(context, JavisService::class.java).apply {
                action = JavisService.ACTION_SPEAK_GREETING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: Exception) {
                // Service may not be running yet — ignore
            }
        }
    }
}
