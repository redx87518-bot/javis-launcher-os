package com.javis.launcher.data.model

/**
 * Permission/safety levels used by the confirmation system (spec #22).
 * Higher ordinal = more sensitive action.
 */
enum class PermissionLevel {
    READ,        // Read screen, notifications, app info
    NAVIGATE,    // Open apps, navigate, search
    INTERACT,    // Tap, type, scroll, select
    COMMUNICATE, // Send messages / emails
    SENSITIVE,   // Delete files, change security settings, account changes
    HIGH_IMPACT // Potentially destructive or externally consequential
}
