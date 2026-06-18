package com.javis.launcher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.javis.launcher.services.JavisForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val serviceIntent = Intent(context, JavisForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
