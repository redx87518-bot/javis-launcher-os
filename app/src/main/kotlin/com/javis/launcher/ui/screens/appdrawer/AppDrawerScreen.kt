package com.javis.launcher.ui.screens.appdrawer

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.data.db.entity.InstalledAppEntity
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.theme.JavisTheme

@Composable
fun AppDrawerScreen(
    onBack: () -> Unit,
    viewModel: AppDrawerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisTheme.colors.background)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = JavisTheme.colors.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Search bar
            GlassCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = JavisTheme.colors.onSurfaceDim, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearch,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        textStyle = JavisTheme.typography.bodyMedium.copy(color = JavisTheme.colors.onBackground),
                        cursorBrush = SolidColor(JavisTheme.colors.primary),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Ask JAVIS or search apps...",
                                    style = JavisTheme.typography.bodyMedium,
                                    color = JavisTheme.colors.onSurfaceDim)
                            }
                            inner()
                        }
                    )
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearch("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear",
                                tint = JavisTheme.colors.onSurfaceDim, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Category tabs
        if (state.searchQuery.isBlank()) {
            ScrollableTabRow(
                selectedTabIndex = state.selectedCategoryIndex,
                containerColor = JavisTheme.colors.background,
                contentColor = JavisTheme.colors.primary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                state.categories.forEachIndexed { idx, cat ->
                    Tab(
                        selected = idx == state.selectedCategoryIndex,
                        onClick = { viewModel.selectCategory(idx) },
                        text = {
                            Text(
                                cat.uppercase(),
                                style = JavisTheme.typography.labelSmall,
                                color = if (idx == state.selectedCategoryIndex)
                                    JavisTheme.colors.primary else JavisTheme.colors.onSurfaceDim
                            )
                        }
                    )
                }
            }
        }

        // App grid
        if (state.apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null,
                        tint = JavisTheme.colors.onSurfaceDim, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No apps found", style = JavisTheme.typography.bodyMedium,
                        color = JavisTheme.colors.onSurfaceDim)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.apps.size) { idx ->
                    DrawerAppItem(
                        app = state.apps[idx],
                        onClick = { viewModel.launchApp(state.apps[idx]) },
                        onLongClick = { viewModel.toggleFavorite(state.apps[idx]) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppItem(
    app: InstalledAppEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(JavisTheme.colors.surfaceVariant)
                .border(
                    if (app.isFavorite) 1.dp else 0.5.dp,
                    if (app.isFavorite) JavisTheme.colors.primary else JavisTheme.colors.glassBorder,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(2).uppercase(),
                style = JavisTheme.typography.titleMedium,
                color = if (app.isFavorite) JavisTheme.colors.primary else JavisTheme.colors.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.appName,
            style = JavisTheme.typography.labelSmall,
            color = JavisTheme.colors.onSurfaceDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
