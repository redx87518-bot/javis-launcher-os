package com.javis.launcher.ui.screens.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.data.db.dao.MemoryDao
import com.javis.launcher.data.db.entity.MemoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryUiState(
    val memories: List<MemoryEntity> = emptyList(),
    val searchQuery: String = "",
    val newKey: String = "",
    val newValue: String = "",
    val newCategory: String = "general",
    val showAddDialog: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryDao: MemoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            memoryDao.observeAll().collect { memories ->
                val query = _uiState.value.searchQuery
                _uiState.update {
                    it.copy(memories = if (query.isBlank()) memories
                    else memories.filter { m ->
                        m.key.contains(query, ignoreCase = true) ||
                        m.value.contains(query, ignoreCase = true)
                    })
                }
            }
        }
    }

    fun onSearchChange(q: String) { _uiState.update { it.copy(searchQuery = q) } }
    fun onNewKeyChange(v: String) { _uiState.update { it.copy(newKey = v) } }
    fun onNewValueChange(v: String) { _uiState.update { it.copy(newValue = v) } }
    fun onNewCategoryChange(v: String) { _uiState.update { it.copy(newCategory = v) } }
    fun toggleAddDialog(show: Boolean) { _uiState.update { it.copy(showAddDialog = show, newKey = "", newValue = "", newCategory = "general") } }

    fun saveMemory() {
        val s = _uiState.value
        if (s.newKey.isBlank() || s.newValue.isBlank()) return
        viewModelScope.launch {
            memoryDao.upsert(MemoryEntity(key = s.newKey, value = s.newValue, category = s.newCategory))
            _uiState.update { it.copy(showAddDialog = false, saveSuccess = true) }
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch { memoryDao.delete(memory) }
    }

    fun clearAll() {
        viewModelScope.launch { memoryDao.deleteAll() }
    }
}
