package com.javis.launcher.ui.home

import android.content.Context
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
import com.javis.launcher.data.local.InstalledAppEntity
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

    val recentTasks = taskDao.getRecentTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentProvider = brainManager.currentProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiProvider.OPENROUTER)

    val currentTask = taskPlanner.currentTask
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isSpeaking = voiceManager.isSpeaking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _greeting = MutableStateFlow(getGreeting())
    val greeting: StateFlow<String> = _greeting.asStateFlow()

    private val _batteryLevel = MutableStateFlow(getBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val userName: String get() = prefs.getString("user_name", "") ?: ""
    val userNickname: String get() = prefs.getString("user_nickname", "Sir") ?: "Sir"

    init {
        refreshApps()
        updateSystemStatus()
    }

    fun onCoreActivated() {
        viewModelScope.launch {
            if (_coreState.value == CoreState.IDLE) {
                _coreState.value = CoreState.LISTENING
                _isListening.value = true
                val greeting = "Welcome back, $userNickname. How may I assist you?"
                _javisResponse.value = greeting
                voiceManager.speak(greeting)
            }
        }
    }

    fun onUserSpoke(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _isListening.value = false
            _coreState.value = CoreState.THINKING
            _javisResponse.value = "Processing..."

            val response = taskPlanner.processUserInput(input)

            _coreState.value = CoreState.SPEAKING
            _javisResponse.value = response
            voiceManager.speak(response)

            kotlinx.coroutines.delay(500)
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
    }

    fun refreshApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            val entities = apps.map { info ->
                InstalledAppEntity(
                    packageName = info.activityInfo.packageName,
                    appName = info.loadLabel(pm).toString(),
                    category = getCategoryForPackage(info.activityInfo.packageName)
                )
            }
            appDao.insertApps(entities)
        }
    }

    private fun updateSystemStatus() {
        viewModelScope.launch {
            _batteryLevel.value = getBatteryLevel()
            _isOnline.value = isNetworkAvailable()
            _greeting.value = getGreeting()
        }
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning, $userNickname."
            hour < 17 -> "Good afternoon, $userNickname."
            hour < 21 -> "Good evening, $userNickname."
            else -> "Welcome back, $userNickname."
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

    private fun getCategoryForPackage(pkg: String): String = when {
        pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("messenger") ||
        pkg.contains("sms") || pkg.contains("message") -> "Messaging"
        pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("spotify") ||
        pkg.contains("music") || pkg.contains("video") -> "Entertainment"
        pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("browser") -> "Browser"
        pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photo") -> "Camera"
        pkg.contains("maps") || pkg.contains("navigation") -> "Navigation"
        pkg.contains("settings") -> "Settings"
        pkg.contains("calculator") -> "Utilities"
        pkg.contains("clock") || pkg.contains("alarm") -> "Clock"
        pkg.contains("email") || pkg.contains("gmail") || pkg.contains("mail") -> "Email"
        pkg.contains("twitter") || pkg.contains("instagram") || pkg.contains("tiktok") ||
        pkg.contains("facebook") -> "Social"
        else -> "Other"
    }
}
