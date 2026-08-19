package com.javis.launcher.ui.appdrawer

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.InstalledAppEntity
import com.javis.launcher.ui.components.JavisBottomNav
import com.javis.launcher.ui.theme.*

@Composable
fun AppDrawerScreen(
    navController: NavController,
    viewModel: AppDrawerViewModel = hiltViewModel()
) {
    val allApps by viewModel.allApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) } // 0 All, 1 Fav, 2 Recent, 3 Cat

    val results = remember(query, allApps) {
        if (query.isBlank()) emptyList() else viewModel.search(query, allApps)
    }

    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JavisBgCard)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("APPS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp, color = JavisRed))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Extension, "Skills", tint = JavisTextSecondary,
                modifier = Modifier.clickable { navController.navigate("skills") }.size(22.dp))
        }

        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(JavisBgCard, RoundedCornerShape(24.dp))
                .border(1.dp, JavisGlassBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = JavisTextDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search apps or ask (e.g. \"messaging app\")", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim))
                    inner()
                }, singleLine = true
            )
            if (query.isNotBlank()) {
                Icon(Icons.Default.Close, "Clear", tint = JavisTextDim,
                    modifier = Modifier.size(18.dp).clickable { query = "" })
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Fav", "Recent", "Cat").forEachIndexed { i, label ->
                val selected = tab == i
                Box(
                    modifier = Modifier
                        .background(if (selected) JavisRed.copy(alpha = 0.15f) else JavisBgCard, RoundedCornerShape(12.dp))
                        .border(1.dp, if (selected) JavisRed.copy(alpha = 0.5f) else JavisGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall.copy(color = if (selected) JavisRed else JavisTextDim))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.isNotBlank() && results.isNotEmpty()) {
                item { Text("RESULTS", style = labelStyle()) }
                item { EntityGrid(apps = results.map { InstalledAppEntity(it.packageName, it.appName, it.category) }, viewModel = viewModel, navController = navController) }
            } else if (query.isNotBlank()) {
                item { Text("No apps match \"$query\".", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim)) }
            } else when (tab) {
                0 -> item { EntityGrid(allApps, viewModel, navController) }
                1 -> if (favoriteApps.isEmpty()) item { EmptyHint("No favorite apps yet.") } else item { EntityGrid(favoriteApps, viewModel, navController) }
                2 -> if (recentApps.isEmpty()) item { EmptyHint("No recent apps.") } else item { EntityGrid(recentApps, viewModel, navController) }
                3 -> {
                    viewModel.categories(allApps).forEach { (cat, list) ->
                        item { Text(cat.uppercase(), style = labelStyle()) }
                        item { EntityGrid(list, viewModel, navController) }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    JavisBottomNav(navController = navController, currentRoute = currentRoute)
}

@Composable
private fun EntityGrid(
    apps: List<InstalledAppEntity>,
    viewModel: AppDrawerViewModel,
    navController: NavController
) {
    val rows = apps.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { app ->
                    AppTile(
                        app = app,
                        onTap = { viewModel.launchApp(app.packageName) },
                        onLongPress = { navController.navigate("app_intelligence/${app.packageName}") }
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AppTile(app: InstalledAppEntity, onTap: () -> Unit, onLongPress: () -> Unit) {
    var fav by remember { mutableStateOf(app.isFavorite) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(JavisBgElevated, RoundedCornerShape(14.dp))
                .border(1.dp, JavisGlassBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(app.appName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge.copy(color = JavisRed))
            if (fav) {
                Icon(Icons.Default.Star, null, tint = JavisGold, modifier = Modifier.align(Alignment.TopEnd).size(14.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(app.appName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun labelStyle() = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp)

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim), textAlign = TextAlign.Center)
    }
}
