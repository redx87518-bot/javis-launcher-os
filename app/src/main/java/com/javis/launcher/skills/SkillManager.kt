package com.javis.launcher.skills

import android.content.SharedPreferences
import com.javis.launcher.data.model.PermissionLevel
import com.javis.launcher.data.model.Skill
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry of the built-in JAVIS skills (spec #18).
 *
 * Skills are the modular unit of capability. The platform is designed so any
 * skill implementation can later be replaced without changing the public
 * architecture. Enabled state is persisted per-skill in SharedPreferences.
 */
@Singleton
class SkillManager @Inject constructor(
    private val prefs: SharedPreferences
) {

    val allSkills: List<Skill> = listOf(
        Skill("app_launcher", "App Control", "Launch and operate installed applications.", "System", PermissionLevel.NAVIGATE, listOf("Accessibility"), supportsTest = true),
        Skill("screen_reader", "Screen Reader", "Read accessible UI content from the current screen.", "Vision", PermissionLevel.READ, listOf("Accessibility")),
        Skill("vision", "Screen Vision", "Understand screenshots and the current screen layout.", "Vision", PermissionLevel.READ, listOf("Accessibility")),
        Skill("whatsapp", "WhatsApp", "Read and draft WhatsApp messages (with confirmation).", "Messaging", PermissionLevel.COMMUNICATE, listOf("Accessibility", "Notifications"), supportsTest = true),
        Skill("messaging", "Messaging", "Send and summarize SMS and messages.", "Messaging", PermissionLevel.COMMUNICATE, listOf("SEND_SMS", "READ_SMS")),
        Skill("email", "Email", "Read, search and draft email replies.", "Productivity", PermissionLevel.COMMUNICATE, listOf("Accessibility")),
        Skill("browser", "Browser", "Open, search and read web pages.", "Web", PermissionLevel.NAVIGATE, emptyList(), supportsTest = true),
        Skill("notifications", "Notifications", "Group, summarize and read notifications aloud.", "System", PermissionLevel.READ, listOf("Notifications")),
        Skill("contacts", "Contacts", "Search and call your contacts.", "System", PermissionLevel.NAVIGATE, listOf("READ_CONTACTS", "CALL_PHONE")),
        Skill("calendar", "Calendar", "Read events and create reminders.", "Productivity", PermissionLevel.NAVIGATE, listOf("READ_CALENDAR")),
        Skill("reminders", "Reminders", "Set alarms and reminders.", "Productivity", PermissionLevel.NAVIGATE, listOf("SET_ALARM")),
        Skill("files", "Files", "Browse and manage local files.", "System", PermissionLevel.INTERACT, listOf("READ_EXTERNAL_STORAGE")),
        Skill("camera", "Camera", "Open the camera and capture photos.", "Media", PermissionLevel.INTERACT, emptyList()),
        Skill("maps", "Maps", "Open maps and navigation.", "Web", PermissionLevel.NAVIGATE, emptyList()),
        Skill("youtube", "YouTube", "Play videos and search YouTube.", "Media", PermissionLevel.NAVIGATE, emptyList()),
        Skill("github", "GitHub", "Browse repos, issues and workflows.", "Developer", PermissionLevel.READ, listOf("Internet")),
        Skill("coding", "Developer", "Explain code and suggest fixes.", "Developer", PermissionLevel.READ, listOf("Internet")),
        Skill("cybersecurity", "Cybersecurity", "Authorized security analysis and SOC tools.", "Developer", PermissionLevel.SENSITIVE, listOf("Internet"), optional = true),
        Skill("system", "System Tools", "Battery, settings and device information.", "System", PermissionLevel.NAVIGATE, emptyList())
    )

    fun getSkill(id: String): Skill? = allSkills.firstOrNull { it.id == id }

    fun isEnabled(id: String): Boolean = prefs.getBoolean("skill_enabled_$id", true)

    fun setEnabled(id: String, enabled: Boolean) {
        prefs.edit().putBoolean("skill_enabled_$id", enabled).apply()
    }

    /** Whether an action at the given level requires explicit user confirmation. */
    fun requiresConfirmation(level: PermissionLevel): Boolean =
        level.ordinal >= PermissionLevel.COMMUNICATE.ordinal

    fun requiredLevelForSkill(id: String): PermissionLevel =
        getSkill(id)?.permissionLevel ?: PermissionLevel.NAVIGATE
}
