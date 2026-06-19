package com.javis.launcher.ui.memory

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.local.MemoryDao
import com.javis.launcher.data.model.MemoryEntity
import com.javis.launcher.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryDao: MemoryDao
) : ViewModel() {

    val allMemory = memoryDao.getAllMemory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryDao.delete(id)
        }
    }

    fun addMemory(key: String, value: String, category: String) {
        viewModelScope.launch {
            memoryDao.insertOrUpdate(
                MemoryEntity(
                    key = key,
                    value = value,
                    category = category,
                    priority = 1
                )
            )
        }
    }
}

@Composable
fun MemoryScreen(
    navController: NavController,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val memories by viewModel.allMemory.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val groupedMemories = memories.groupBy { it.category }

    val categoryColors = mapOf(
        "profile" to JavisRed,
        "habit" to Color(0xFF00D2FF),
        "preference" to Color(0xFFFFD700),
        "routine" to Color(0xFF00FF88),
        "fact" to Color(0xFFFF6B6B)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = JavisRed)
            }
            Text(
                "MEMORY CORE",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = JavisRed,
                    letterSpacing = 3.sp
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add memory", tint = JavisRed)
            }
        }

        HorizontalDivider(color = JavisGlassBorder)

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = JavisRed.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No memories stored yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "JAVIS will remember things as you talk.\nYou can also add memories manually.",
                        style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Stats bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(JavisBgCard, RoundedCornerShape(12.dp))
                            .border(1.dp, JavisGlassBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MemoryStat("TOTAL", "${memories.size}", JavisRed)
                        MemoryStat("CATEGORIES", "${groupedMemories.keys.size}", Color(0xFF00D2FF))
                        MemoryStat("PRIORITY", "${memories.count { it.priority > 0 }}", Color(0xFFFFD700))
                    }
                }

                // Groups
                groupedMemories.forEach { (category, items) ->
                    item {
                        val catColor = categoryColors[category.lowercase()] ?: JavisTextDim
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(JavisBgCard, RoundedCornerShape(12.dp))
                                .border(1.dp, catColor.copy(0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(catColor.copy(0.1f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    getCategoryIcon(category),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    category.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = catColor,
                                        letterSpacing = 2.sp
                                    )
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${items.size} items",
                                    style = MaterialTheme.typography.labelSmall.copy(color = catColor.copy(0.7f))
                                )
                            }

                            items.forEachIndexed { idx, memory ->
                                if (idx > 0) HorizontalDivider(
                                    color = JavisGlassBorder.copy(0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                MemoryItem(
                                    memory = memory,
                                    accentColor = catColor,
                                    onDelete = { viewModel.deleteMemory(memory.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { key, value, category ->
                viewModel.addMemory(key, value, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MemoryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(color = color))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            color = JavisTextDim, letterSpacing = 1.sp
        ))
    }
}

@Composable
private fun MemoryItem(memory: MemoryEntity, accentColor: Color, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                memory.key,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = accentColor.copy(0.9f)
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                memory.value,
                style = MaterialTheme.typography.bodySmall.copy(color = JavisTextPrimary)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                fmt.format(Date(memory.timestamp)),
                style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = JavisTextDim,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AddMemoryDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("preference") }
    val categories = listOf("profile", "habit", "preference", "routine", "fact")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JavisBgCard,
        titleContentColor = JavisRed,
        textContentColor = JavisTextPrimary,
        title = { Text("Add Memory", letterSpacing = 2.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key (e.g. 'favorite color')") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JavisRed,
                        unfocusedBorderColor = JavisGlassBorder,
                        focusedTextColor = JavisTextPrimary,
                        unfocusedTextColor = JavisTextPrimary,
                        focusedLabelColor = JavisRed,
                        unfocusedLabelColor = JavisTextDim,
                        cursorColor = JavisRed
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value (e.g. 'blue')") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JavisRed,
                        unfocusedBorderColor = JavisGlassBorder,
                        focusedTextColor = JavisTextPrimary,
                        unfocusedTextColor = JavisTextPrimary,
                        focusedLabelColor = JavisRed,
                        unfocusedLabelColor = JavisTextDim,
                        cursorColor = JavisRed
                    )
                )
                Text("Category", style = MaterialTheme.typography.labelMedium.copy(color = JavisTextDim))
                Row(
                    modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JavisRed.copy(0.2f),
                                selectedLabelColor = JavisRed,
                                containerColor = JavisBgElevated,
                                labelColor = JavisTextDim
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(key.trim(), value.trim(), category)
                    }
                }
            ) {
                Text("SAVE", color = JavisRed, letterSpacing = 2.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = JavisTextDim, letterSpacing = 1.sp)
            }
        }
    )
}

private fun getCategoryIcon(category: String) = when (category.lowercase()) {
    "profile" -> Icons.Default.Person
    "habit" -> Icons.Default.Loop
    "preference" -> Icons.Default.Tune
    "routine" -> Icons.Default.Schedule
    else -> Icons.Default.BookmarkBorder
}

