package com.javis.launcher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.brain.BrainManager
import com.javis.launcher.data.local.AppDao
import com.javis.launcher.data.local.FavoriteContactDao
import com.javis.launcher.data.local.NotificationCacheDao
import com.javis.launcher.data.local.TaskDao
import com.javis.launcher.data.model.InstalledAppEntity
import com.javis.launcher.data.model.*
import com.javis.launcher.tasks.TaskPlanner
import com.javis.launcher.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val brainManager: BrainManager,
    private val voiceManager: VoiceManager,
    private val taskPlanner: TaskPlanner,
    private val appDao: AppDao,
    private val contactDao: FavoriteContactDao,
    private val notificationDao: NotificationCacheDao,
    private val taskDao: TaskDao,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _coreState = MutableStateFlow(CoreState.IDLE)
    val coreState: StateFlow<CoreState> = _coreState.asStateFlow()

    private val _javisResponse = MutableStateFlow("")
    val javisResponse: StateFlow<String> = _javisResponse.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AppInfo>>(emptyList())
    val searchResults: StateFlow<List<AppInfo>> = _searchResults.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastInput = MutableStateFlow("")
    val lastInput: StateFlow<String> = _lastInput.asStateFlow()

    val favoriteApps = appDao.getFavoriteApps().map { list ->
        list.map { AppInfo(it.packageName, it.appName, it.category, it.launchCount, it.isFavorite) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentApps = appDao.getRecentApps().map { list ->
        list.map { AppInfo(it.packageName, it.appName, it.category, it.launchCount, it.isFavorite) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts = contactDao.getFavoriteContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount = notificationDao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadNotifications = notificationDao.getUnreadNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTasks = taskDao.getRecentTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProvider = brainManager.currentProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProvider.GROQ)

    val currentTask = taskPlanner.currentTask
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isSpeaking = voiceManager.isSpeaking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val elevenLabsStatus = voiceManager.elevenLabsStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceManager.ElevenLabsStatus.UNKNOWN)

    private val _greeting = MutableStateFlow(getGreeting())
    val greeting: StateFlow<String> = _greeting.asStateFlow()

    private val _batteryLevel = MutableStateFlow(getBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val userName: String get() = prefs.getString("user_name", "") ?: ""
    val userNickname: String get() = prefs.getString("user_nickname", "boss") ?: "boss"
    val hasElevenLabsKey: Boolean get() = (prefs.getString("elevenlabs_api_key", "") ?: "").isNotBlank()

    // Broadcast receiver for unlock greeting
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.javis.launcher.DAILY_BRIEFING") {
                greetOnUnlock()
            }
        }
    }

    init {
        refreshApps()
        updateSystemStatus()
        registerUnlockReceiver()
    }

    private fun registerUnlockReceiver() {
        try {
            val filter = IntentFilter("com.javis.launcher.DAILY_BRIEFING")
            context.registerReceiver(unlockReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun greetOnUnlock() {
        viewModelScope.launch {
            val greeting = buildUnlockGreeting()
            _greeting.value = greeting.substringBefore(",").trim() + ", $userNickname."
            _javisResponse.value = greeting
            voiceManager.speak(greeting)
        }
    }

    private fun buildUnlockGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        return "$timeGreeting, $userNickname. Welcome back. How can I assist you today?"
    }

    fun onCoreActivated() {
        viewModelScope.launch {
            if (_coreState.value == CoreState.IDLE || _coreState.value == CoreState.COMPLETED) {
                _coreState.value = CoreState.LISTENING
                _isListening.value = true
                val msg = "Listening, $userNickname."
                _javisResponse.value = msg
                voiceManager.speak(msg)
            }
        }
    }

    fun onUserSpoke(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _isListening.value = false
            _lastInput.value = input
            _coreState.value = CoreState.THINKING
            _javisResponse.value = "Processing..."

            val response = taskPlanner.processUserInput(input)

            _coreState.value = CoreState.SPEAKING
            _javisResponse.value = response
            voiceManager.speak(response)

            kotlinx.coroutines.delay(800)
            _coreState.value = CoreState.IDLE
        }
    }

    fun onTextInput(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _lastInput.value = input
            _coreState.value = CoreState.THINKING
            _javisResponse.value = "Thinking..."

            val response = taskPlanner.processUserInput(input)

            _coreState.value = CoreState.SPEAKING
            _javisResponse.value = response
            voiceManager.speak(response)

            kotlinx.coroutines.delay(800)
            _coreState.value = CoreState.IDLE
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val results = appDao.searchApps(query)
            _searchResults.value = results.map {
                AppInfo(it.packageName, it.appName, it.category, it.launchCount, it.isFavorite)
            }
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            try {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(packageName)
                intent?.let {
                    it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    appDao.incrementLaunch(packageName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun callContact(phoneNumber: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:$phoneNumber")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissResponse() {
        _javisResponse.value = ""
        _coreState.value = CoreState.IDLE
        voiceManager.stopSpeaking()
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            notificationDao.markRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            notificationDao.markAllRead()
        }
    }

    fun clearConversation() {
        brainManager.clearConversation()
        _javisResponse.value = ""
    }

    fun refreshApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val activities = pm.queryIntentActivities(intent, 0)
            val appList = activities.mapNotNull { ri ->
                try {
                    InstalledAppEntity(
                        packageName = ri.activityInfo.packageName,
                        appName = pm.getApplicationLabel(ri.activityInfo.applicationInfo).toString(),
                        category = getAppCategory(ri.activityInfo.packageName)
                    )
                } catch (e: Exception) { null }
            }
            if (appList.isNotEmpty()) {
                appDao.insertApps(appList)
            }
        }
    }

    private fun updateSystemStatus() {
        viewModelScope.launch {
            while (true) {
                _batteryLevel.value = getBatteryLevel()
                _isOnline.value = isNetworkAvailable()
                _greeting.value = getGreeting()
                kotlinx.coroutines.delay(60_000)
            }
        }
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val name = prefs.getString("user_name", "") ?: ""
        val display = if (name.isNotBlank()) name else (prefs.getString("user_nickname", "") ?: "")
        val nameStr = if (display.isNotBlank()) ", $display" else ""
        return when {
            hour < 5 -> "Late night$nameStr."
            hour < 12 -> "Good morning$nameStr."
            hour < 17 -> "Good afternoon$nameStr."
            hour < 21 -> "Good evening$nameStr."
            else -> "Good night$nameStr."
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getAppCategory(pkg: String): String = when {
        pkg.contains("camera") -> "Camera"
        pkg.contains("gallery") || pkg.contains("photos") -> "Photos"
        pkg.contains("music") || pkg.contains("spotify") || pkg.contains("soundcloud") -> "Music"
        pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("prime") -> "Video"
        pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("messenger") -> "Social"
        pkg.contains("instagram") || pkg.contains("twitter") || pkg.contains("tiktok") || pkg.contains("facebook") -> "Social"
        pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("browser") -> "Browser"
        pkg.contains("gmail") || pkg.contains("outlook") || pkg.contains("email") -> "Email"
        pkg.contains("maps") || pkg.contains("uber") || pkg.contains("lyft") -> "Navigation"
        pkg.contains("clock") || pkg.contains("calendar") -> "Productivity"
        pkg.contains("settings") -> "System"
        pkg.contains("game") || pkg.contains("play") -> "Games"
        else -> "App"
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(unlockReceiver) } catch (_: Exception) {}
    }
}
