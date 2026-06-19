package com.javis.launcher.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.*
import com.javis.launcher.ui.home.components.*
import com.javis.launcher.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val coreState by viewModel.coreState.collectAsState()
    val greeting by viewModel.greeting.collectAsState()
    val javisResponse by viewModel.javisResponse.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val favoriteContacts by viewModel.favoriteContacts.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val currentProvider by viewModel.currentProvider.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentTask by viewModel.currentTask.collectAsState()
    val unreadNotifications by viewModel.unreadNotifications.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    // Voice input launcher
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spokenText = results?.firstOrNull() ?: ""
        if (spokenText.isNotBlank()) {
            viewModel.onUserSpoke(spokenText)
        }
    }

    fun activateVoice() {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        viewModel.onCoreActivated()
        try {
            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JAVIS...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            viewModel.dismissResponse()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
    ) {
        // Background grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = Color(0x0A4488FF)
            val cellSize = 60.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
                x += cellSize
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                y += cellSize
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Bar
            item {
                StatusBar(
                    batteryLevel = batteryLevel,
                    isOnline = isOnline,
                    currentProvider = currentProvider,
                    unreadCount = unreadCount,
                    onSettingsTap = { navController.navigate("settings") },
                    onMissionTap = { navController.navigate("mission") }
                )
            }

            // Greeting
            item {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = JavisTextPrimary,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // JAVIS Response bubble
            if (javisResponse.isNotBlank()) {
                item {
                    AnimatedVisibility(
                        visible = javisResponse.isNotBlank(),
                        enter = fadeIn() + slideInVertically { -20 },
                        exit = fadeOut() + slideOutVertically { -20 }
                    ) {
                        ResponseBubble(
                            text = javisResponse,
                            isActive = coreState != CoreState.IDLE,
                            onDismiss = { viewModel.dismissResponse() }
                        )
                    }
                }
            }

            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { viewModel.onUserSpoke("search for $it") }
                )
            }

            // Search results
            if (searchResults.isNotEmpty()) {
                item {
                    SearchResultsPanel(
                        results = searchResults,
                        onAppTap = { viewModel.launchApp(it.packageName) }
                    )
                }
            }

            // AI Core
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AiCore(
                            state = coreState,
                            onTap = { activateVoice() }
                        )
                        Spacer(Modifier.height(8.dp))
                        CoreStateLabel(state = coreState)
                        if (currentTask.isNotBlank() && coreState == CoreState.EXECUTING) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentTask,
                                style = MaterialTheme.typography.bodySmall.copy(color = JavisGold),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Text input for JAVIS
            item {
                JavisTextInput(
                    onSubmit = { viewModel.onTextInput(it) },
                    isProcessing = coreState == CoreState.THINKING || coreState == CoreState.SPEAKING
                )
            }

            // Quick nav: Chat | Memory | Notifications
            item {
                JavisNavRow(
                    unreadCount = unreadCount,
                    onChatTap = { navController.navigate("conversation") },
                    onMemoryTap = { navController.navigate("memory") },
                    onNotifTap = { navController.navigate("conversation") }
                )
            }

            // Unread notifications panel
            if (unreadNotifications.isNotEmpty()) {
                item {
                    SectionHeader(title = "NOTIFICATIONS")
                    Spacer(Modifier.height(8.dp))
                }
                items(unreadNotifications.take(3)) { notif ->
                    NotificationItem(
                        appName = notif.appName,
                        title = notif.title,
                        text = notif.text,
                        onTap = { viewModel.markNotificationRead(notif.id) }
                    )
                }
                if (unreadNotifications.size > 3) {
                    item {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsRead() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Mark all ${unreadNotifications.size} read",
                                style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim)
                            )
                        }
                    }
                }
            }

            // Favorite Apps
            if (favoriteApps.isNotEmpty()) {
                item {
                    SectionHeader(title = "FAVORITE APPS")
                    Spacer(Modifier.height(8.dp))
                    AppGrid(
                        apps = favoriteApps,
                        onAppTap = { viewModel.launchApp(it.packageName) }
                    )
                }
            }

            // Recent Apps
            if (recentApps.isNotEmpty()) {
                item {
                    SectionHeader(title = "RECENT")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentApps.take(8)) { app ->
                            AppChip(app = app, onTap = { viewModel.launchApp(app.packageName) })
                        }
                    }
                }
            }

            // Favorite Contacts
            if (favoriteContacts.isNotEmpty()) {
                item {
                    SectionHeader(title = "CONTACTS")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteContacts) { contact ->
                            ContactCard(
                                name = contact.name,
                                onCall = { viewModel.callContact(contact.phoneNumber) }
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                SectionHeader(title = "QUICK ACTIONS")
                Spacer(Modifier.height(8.dp))
                QuickActionsRow(
                    onCallTap = { activateVoice() },
                    onSearchTap = { activateVoice() },
                    onAlarmTap = { viewModel.onUserSpoke("open alarm") },
                    onSettingsTap = { navController.navigate("settings") }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun StatusBar(
    batteryLevel: Int,
    isOnline: Boolean,
    currentProvider: AiProvider,
    unreadCount: Int,
    onSettingsTap: () -> Unit,
    onMissionTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isOnline) JavisGreen else JavisRed, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = currentProvider.name,
                style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary)
            )
        }

        // Center - JAVIS label
        Text(
            text = "J·A·V·I·S",
            style = MaterialTheme.typography.titleMedium.copy(
                color = JavisRed,
                letterSpacing = 4.sp
            )
        )

        // Right actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (unreadCount > 0) {
                Box {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = JavisTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(JavisRed, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = "$batteryLevel%",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = when {
                        batteryLevel > 50 -> JavisGreen
                        batteryLevel > 20 -> JavisGold
                        else -> JavisRed
                    }
                )
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Dashboard,
                contentDescription = "Mission Control",
                tint = JavisTextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onMissionTap() }
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = JavisTextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onSettingsTap() }
            )
        }
    }
}

@Composable
fun ResponseBubble(text: String, isActive: Boolean, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "resp")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "border"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0x1A0066FF), Color(0x1A4400CC), Color(0x1A000000))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                JavisBlue.copy(alpha = if (isActive) borderAlpha else 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(listOf(JavisRed, JavisBlue)),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JAVIS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = JavisRed,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = JavisTextDim,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDismiss() }
            )
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(24.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = JavisTextDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Search apps, contacts, web...", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim))
                inner()
            },
            singleLine = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch(query) }
            )
        )
        if (query.isNotBlank()) {
            Icon(
                Icons.Default.Close, contentDescription = "Clear",
                tint = JavisTextDim, modifier = Modifier.size(18.dp).clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
fun SearchResultsPanel(results: List<AppInfo>, onAppTap: (AppInfo) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        results.take(6).forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppTap(app) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(JavisGlass, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(color = JavisRed)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(app.appName, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
                    Text(app.category, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(JavisRed, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = JavisTextSecondary,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
fun AppGrid(apps: List<AppInfo>, onAppTap: (AppInfo) -> Unit) {
    val rows = apps.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { app ->
                    AppIconItem(app = app, onTap = { onAppTap(app) }, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AppIconItem(app: AppInfo, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onTap() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(JavisBgElevated, RoundedCornerShape(14.dp))
                .border(1.dp, JavisGlassBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(color = JavisRed)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppChip(app: AppInfo, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .background(JavisBgCard, RoundedCornerShape(20.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(20.dp))
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(JavisGlass, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(app.appName.take(1), style = MaterialTheme.typography.bodySmall.copy(color = JavisRed))
        }
        Spacer(Modifier.width(6.dp))
        Text(app.appName, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextPrimary), maxLines = 1)
    }
}

@Composable
fun ContactCard(name: String, onCall: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(JavisBgCard, RoundedCornerShape(12.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
            .clickable { onCall() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.radialGradient(listOf(JavisRed.copy(0.3f), JavisRedDark.copy(0.1f))),
                    CircleShape
                )
                .border(1.dp, JavisRed.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium.copy(color = JavisRed))
        }
        Spacer(Modifier.height(4.dp))
        Text(name.split(" ").first(), style = MaterialTheme.typography.bodySmall.copy(color = JavisTextPrimary))
        Icon(Icons.Default.Call, contentDescription = null, tint = JavisGreenDim, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun QuickActionsRow(
    onCallTap: () -> Unit,
    onSearchTap: () -> Unit,
    onAlarmTap: () -> Unit,
    onSettingsTap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple(Icons.Default.Call, "CALL", onCallTap),
            Triple(Icons.Default.Search, "SEARCH", onSearchTap),
            Triple(Icons.Default.Alarm, "ALARM", onAlarmTap),
            Triple(Icons.Default.Settings, "SETTINGS", onSettingsTap)
        ).forEach { (icon, label, action) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .background(JavisBgCard, RoundedCornerShape(12.dp))
                    .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
                    .clickable { action() }
                    .padding(vertical = 12.dp)
            ) {
                Icon(icon, contentDescription = label, tint = JavisRed, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim))
            }
        }
    }
}

@Composable
fun JavisTextInput(onSubmit: (String) -> Unit, isProcessing: Boolean) {
    var text by remember { mutableStateOf("") }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(24.dp))
            .border(1.dp, JavisRed.copy(0.4f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = JavisRed.copy(0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary),
            decorationBox = { inner ->
                if (text.isEmpty()) Text("Type a command or question...", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim))
                inner()
            },
            singleLine = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        onSubmit(text.trim())
                        text = ""
                        keyboard?.hide()
                    }
                }
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done)
        )
        Spacer(Modifier.width(8.dp))
        if (isProcessing) {
            CircularProgressIndicator(color = JavisRed, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                Icons.Default.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) JavisRed else JavisTextDim,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        if (text.isNotBlank()) {
                            onSubmit(text.trim())
                            text = ""
                            keyboard?.hide()
                        }
                    }
            )
        }
    }
}

@Composable
fun JavisNavRow(unreadCount: Int, onChatTap: () -> Unit, onMemoryTap: () -> Unit, onNotifTap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chat History
        Row(
            modifier = Modifier
                .weight(1f)
                .background(JavisBgCard, RoundedCornerShape(12.dp))
                .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
                .clickable { onChatTap() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, tint = JavisRed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("CHAT", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim, letterSpacing = 1.sp))
        }
        // Memory
        Row(
            modifier = Modifier
                .weight(1f)
                .background(JavisBgCard, RoundedCornerShape(12.dp))
                .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
                .clickable { onMemoryTap() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF00D2FF), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("MEMORY", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim, letterSpacing = 1.sp))
        }
        // Notifications
        Box(
            modifier = Modifier
                .weight(1f)
                .background(JavisBgCard, RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (unreadCount > 0) JavisRed.copy(0.5f) else JavisGlassBorder,
                    RoundedCornerShape(12.dp)
                )
                .clickable { onNotifTap() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = if (unreadCount > 0) JavisRed else JavisTextDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (unreadCount > 0) "$unreadCount" else "NOTIF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (unreadCount > 0) JavisRed else JavisTextDim,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

@Composable
fun NotificationItem(appName: String, title: String, text: String, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(10.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(10.dp))
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(JavisRed, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, style = MaterialTheme.typography.labelSmall.copy(color = JavisRed, letterSpacing = 1.sp))
            if (title.isNotBlank()) {
                Text(title, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (text.isNotBlank()) {
                Text(text, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = JavisTextDim, modifier = Modifier.size(14.dp))
    }
}
