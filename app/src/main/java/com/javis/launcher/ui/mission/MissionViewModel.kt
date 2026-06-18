package com.javis.launcher.ui.mission

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.brain.BrainManager
import com.javis.launcher.data.local.NotificationCacheDao
import com.javis.launcher.data.local.TaskDao
import com.javis.launcher.data.model.*
import com.javis.launcher.tasks.TaskPlanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val brainManager: BrainManager,
    private val taskPlanner: TaskPlanner,
    private val taskDao: TaskDao,
    private val notificationDao: NotificationCacheDao
) : ViewModel() {

    private val _javisState = MutableStateFlow(JavisState())
    val javisState: StateFlow<JavisState> = _javisState.asStateFlow()

    val recentTasks = taskDao.getRecentTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifications = notificationDao.getUnreadNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startStatusPolling()
    }

    private fun startStatusPolling() {
        viewModelScope.launch {
            while (true) {
                _javisState.value = JavisState(
                    coreState = taskPlanner.taskStatus.value,
                    currentTask = taskPlanner.currentTask.value,
                    isOnline = isNetworkAvailable(),
                    currentProvider = brainManager.currentProvider.value,
                    batteryLevel = getBatteryLevel(),
                    unreadNotifications = notificationDao.getUnreadCount().first()
                )
                delay(3000)
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
