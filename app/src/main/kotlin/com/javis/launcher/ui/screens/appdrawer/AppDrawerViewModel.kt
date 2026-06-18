package com.javis.launcher.ui.screens.appdrawer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.data.db.dao.InstalledAppDao
import com.javis.launcher.data.db.entity.InstalledAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDrawerUiState(
    val apps: List<InstalledAppEntity> = emptyList(),
    val searchQuery: String = "",
    val categories: List<String> = listOf("all", "social", "entertainment", "productivity", "games", "other"),
    val selectedCategoryIndex: Int = 0
)

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppDao: InstalledAppDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        observeApps()
    }

    private fun observeApps() {
        viewModelScope.launch {
            installedAppDao.observeAll().collect { all ->
                filterAndUpdate(all)
            }
        }
    }

    private fun filterAndUpdate(all: List<InstalledAppEntity>) {
        val query = _uiState.value.searchQuery
        val catIdx = _uiState.value.selectedCategoryIndex
        val cats = _uiState.value.categories

        val filtered = when {
            query.isNotBlank() -> all.filter { app ->
                app.appName.contains(query, ignoreCase = true) ||
                app.keywords.contains(query, ignoreCase = true)
            }
            catIdx == 0 -> all
            else -> all.filter { it.category == cats[catIdx] }
        }.sortedWith(compareByDescending<InstalledAppEntity> { it.isFavorite }.thenBy { it.appName })

        _uiState.update { it.copy(apps = filtered) }
    }

    fun onSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val all = mutableListOf<InstalledAppEntity>()
            installedAppDao.observeAll().first().let { all.addAll(it) }
            filterAndUpdate(all)
        }
    }

    fun selectCategory(index: Int) {
        _uiState.update { it.copy(selectedCategoryIndex = index) }
        viewModelScope.launch {
            val all = mutableListOf<InstalledAppEntity>()
            installedAppDao.observeAll().first().let { all.addAll(it) }
            filterAndUpdate(all)
        }
    }

    fun launchApp(app: InstalledAppEntity) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(app.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        viewModelScope.launch { installedAppDao.recordLaunch(app.packageName) }
    }

    fun toggleFavorite(app: InstalledAppEntity) {
        viewModelScope.launch {
            installedAppDao.setFavorite(app.packageName, !app.isFavorite)
        }
    }
}
