package com.javis.launcher.ui.skills

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.data.model.PermissionLevel
import com.javis.launcher.data.model.Skill
import com.javis.launcher.ui.theme.*

@Composable
fun SkillsScreen(
    navController: NavController,
    viewModel: SkillsViewModel = hiltViewModel()
) {
    val enabled by viewModel.enabled.collectAsState()
    val enabledCount = enabled.count { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JavisBgCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("SKILLS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp, color = JavisRed))
                Text("$enabledCount of ${viewModel.skills.size} enabled",
                    style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(viewModel.skills, key = { it.id }) { skill ->
                SkillCard(
                    skill = skill,
                    isEnabled = enabled[skill.id] ?: true,
                    onToggle = { viewModel.setEnabled(skill.id, it) },
                    onTap = { navController.navigate("skill_detail/${skill.id}") }
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SkillCard(
    skill: Skill,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JavisBgCard, RoundedCornerShape(14.dp))
            .border(1.dp, JavisGlassBorder, RoundedCornerShape(14.dp))
            .clickable { onTap() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isEnabled) JavisRed.copy(alpha = 0.15f) else JavisGlass,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = skillIcon(skill.id),
                contentDescription = null,
                tint = if (isEnabled) JavisRed else JavisTextDim,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(skill.name, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
                if (skill.optional) {
                    Spacer(Modifier.width(6.dp))
                    Text("OPTIONAL", style = MaterialTheme.typography.labelSmall.copy(color = JavisGold, fontSize = 8.sp))
                }
            }
            Text(skill.description, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim), maxLines = 2)
            Spacer(Modifier.height(4.dp))
            PermissionBadge(level = skill.permissionLevel)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JavisRed,
                checkedTrackColor = JavisRed.copy(alpha = 0.4f),
                uncheckedThumbColor = JavisTextDim,
                uncheckedTrackColor = JavisGlass
            )
        )
    }
}

@Composable
fun PermissionBadge(level: PermissionLevel) {
    val color = when (level) {
        PermissionLevel.READ -> JavisGreen
        PermissionLevel.NAVIGATE -> JavisBlue
        PermissionLevel.INTERACT -> JavisGold
        PermissionLevel.COMMUNICATE -> JavisRed
        PermissionLevel.SENSITIVE -> Color(0xFFFF8C00)
        PermissionLevel.HIGH_IMPACT -> Color(0xFFFF4444)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            level.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall.copy(color = color, fontSize = 9.sp)
        )
    }
}

@Composable
fun SkillDetailScreen(
    navController: NavController,
    skillId: String,
    viewModel: SkillsViewModel = hiltViewModel()
) {
    val skill = viewModel.getSkill(skillId)
    val enabled by viewModel.enabled.collectAsState()
    val isEnabled = enabled[skillId] ?: true

    if (skill == null) {
        Box(Modifier.fillMaxSize().background(JavisBg), contentAlignment = Alignment.Center) {
            Text("Skill not found.", color = JavisTextDim)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JavisBgCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(skill.name, style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp))
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(skillIcon(skill.id), null, tint = JavisRed, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(skill.category, style = MaterialTheme.typography.labelSmall.copy(color = JavisTextDim))
                        PermissionBadge(level = skill.permissionLevel)
                    }
                }
            }
            item {
                Text(skill.description, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
            }
            item {
                Text("ENABLED", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JavisBgElevated, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Skill active", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary), modifier = Modifier.weight(1f))
                    Switch(checked = isEnabled, onCheckedChange = { viewModel.setEnabled(skill.id, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = JavisRed, checkedTrackColor = JavisRed.copy(alpha = 0.4f)))
                }
            }
            item {
                Text("REQUIRED PERMISSIONS", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
                Spacer(Modifier.height(8.dp))
                if (skill.permissions.isEmpty()) {
                    Text("No special permissions required.", style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim))
                } else {
                    skill.permissions.forEach { perm ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Lock, null, tint = JavisTextDim, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(perm, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextPrimary))
                        }
                    }
                }
            }
            item {
                val needsConfirm = viewModel.requiresConfirmation(skill)
                Text("CONFIRMATION POLICY", style = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp))
                Spacer(Modifier.height(8.dp))
                Text(
                    if (needsConfirm) "Actions from this skill require explicit confirmation before execution."
                    else "Actions from this skill may run without extra confirmation.",
                    style = MaterialTheme.typography.bodySmall.copy(color = JavisTextPrimary)
                )
            }
            if (skill.supportsTest) {
                item {
                    Button(
                        onClick = { /* test action hook (no-op placeholder) */ },
                        colors = ButtonDefaults.buttonColors(containerColor = JavisRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Run self-test")
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun skillIcon(id: String) = when (id) {
    "app_launcher" -> Icons.Default.Android
    "screen_reader", "vision" -> Icons.Default.Visibility
    "whatsapp", "messaging" -> Icons.Default.Chat
    "email" -> Icons.Default.Email
    "browser", "maps", "youtube" -> Icons.Default.Language
    "notifications" -> Icons.Default.Notifications
    "contacts" -> Icons.Default.Contacts
    "calendar", "reminders" -> Icons.Default.DateRange
    "files" -> Icons.Default.Folder
    "camera" -> Icons.Default.CameraAlt
    "github", "coding", "cybersecurity" -> Icons.Default.Code
    "system" -> Icons.Default.Settings
    else -> Icons.Default.Star
}
