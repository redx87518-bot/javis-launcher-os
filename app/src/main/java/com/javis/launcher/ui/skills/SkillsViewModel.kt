package com.javis.launcher.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.javis.launcher.data.model.Skill
import com.javis.launcher.skills.SkillManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val skillManager: SkillManager
) : ViewModel() {

    val skills: List<Skill> = skillManager.allSkills

    private val _enabled = MutableStateFlow(
        skillManager.allSkills.associate { it.id to skillManager.isEnabled(it.id) }
    )
    val enabled: StateFlow<Map<String, Boolean>> = _enabled.asStateFlow()

    fun setEnabled(id: String, value: Boolean) {
        skillManager.setEnabled(id, value)
        val next = _enabled.value.toMutableMap()
        next[id] = value
        _enabled.value = next
    }

    fun getSkill(id: String): Skill? = skillManager.getSkill(id)

    fun requiresConfirmation(skill: Skill): Boolean =
        skillManager.requiresConfirmation(skill.permissionLevel)
}
