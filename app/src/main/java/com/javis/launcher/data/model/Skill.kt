package com.javis.launcher.data.model

/**
 * Declarative description of a JAVIS capability (spec #18).
 * Every major subsystem is exposed as a Skill so the platform stays modular
 * and permission/confirmation policy can be applied uniformly.
 */
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val permissionLevel: PermissionLevel,
    val permissions: List<String> = emptyList(),
    val optional: Boolean = false,
    val supportsTest: Boolean = false
)
