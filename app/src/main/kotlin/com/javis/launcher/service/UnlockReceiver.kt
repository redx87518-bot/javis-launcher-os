package com.javis.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.javis.launcher.data.preferences.JavisPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: JavisPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            CoroutineScope(Dispatchers.IO).launch {
                preferences.recordUnlock()
            }
        }
    }
}
