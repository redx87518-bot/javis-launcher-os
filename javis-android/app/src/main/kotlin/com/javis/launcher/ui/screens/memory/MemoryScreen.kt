package com.javis.launcher.ui.screens.memory

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.launcher.data.db.entity.MemoryEntity
import com.javis.launcher.ui.components.GlassCard
import com.javis.launcher.ui.theme.JavisTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(JavisTheme.colors.background)) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = JavisTheme.colors.onSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("MEMORY", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.primary)
                    Text("${state.memories.size} stored facts", style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim)
                }
                IconButton(onClick = { viewModel.toggleAddDialog(true) }) {
                    Icon(Icons.Default.Add, null, tint = JavisTheme.colors.primary)
                }
            }

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JavisTheme.colors.surfaceVariant)
                    .border(0.5.dp, JavisTheme.colors.glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = JavisTheme.colors.onSurfaceDim, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        textStyle = JavisTheme.typography.bodyMedium.copy(color = JavisTheme.colors.onBackground),
                        cursorBrush = SolidColor(JavisTheme.colors.primary),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) Text("Search memories…", style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurfaceDim)
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.memories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Memory, null, tint = JavisTheme.colors.primary.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No memories yet", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.onSurfaceDim)
                        Text("Tell JAVIS things about yourself to remember", style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onSurfaceDim.copy(alpha = 0.6f))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = state.memories.groupBy { it.category }
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                category.uppercase(),
                                style = JavisTheme.typography.labelSmall,
                                color = JavisTheme.colors.onSurfaceDim,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(items, key = { it.id }) { memory ->
                            MemoryCard(memory = memory, onDelete = { viewModel.deleteMemory(memory) })
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Add memory dialog
        if (state.showAddDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleAddDialog(false) },
                containerColor = JavisTheme.colors.surface,
                title = {
                    Text("Add Memory", style = JavisTheme.typography.titleMedium, color = JavisTheme.colors.onBackground)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MemoryInputField("Key (e.g. user_name)", state.newKey, viewModel::onNewKeyChange)
                        MemoryInputField("Value (e.g. John)", state.newValue, viewModel::onNewValueChange)
                        MemoryInputField("Category (general/personal/interest)", state.newCategory, viewModel::onNewCategoryChange)
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::saveMemory) {
                        Text("Save", color = JavisTheme.colors.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleAddDialog(false) }) {
                        Text("Cancel", color = JavisTheme.colors.onSurfaceDim)
                    }
                }
            )
        }
    }
}

@Composable
private fun MemoryCard(memory: MemoryEntity, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(memory.key.replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.primary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(memory.value, style = JavisTheme.typography.bodyMedium, color = JavisTheme.colors.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Used ${memory.usageCount}×  •  ${formatDate(memory.lastUsed)}",
                    style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = JavisTheme.colors.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MemoryInputField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = JavisTheme.typography.labelSmall, color = JavisTheme.colors.onSurfaceDim, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(JavisTheme.colors.surfaceVariant)
                .border(0.5.dp, JavisTheme.colors.glassBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = JavisTheme.typography.bodyMedium.copy(color = JavisTheme.colors.onBackground),
                cursorBrush = SolidColor(JavisTheme.colors.primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
