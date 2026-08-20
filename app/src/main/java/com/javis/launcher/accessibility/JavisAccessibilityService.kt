package com.javis.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JavisAccessibilityService : AccessibilityService() {

    private var currentPackage: String = ""

    companion object {
        /** Static handle so the UI can request a live screen description. */
        var instance: JavisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            notificationTimeout = 100
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != currentPackage) {
                currentPackage = pkg
                broadcastCurrentApp(pkg)
            }
        }
    }

    override fun onInterrupt() {}

    private fun broadcastCurrentApp(packageName: String) {
        val intent = android.content.Intent("com.javis.launcher.APP_CHANGED").apply {
            putExtra("package", packageName)
            setPackage("com.javis.launcher")
        }
        sendBroadcast(intent)
    }

    fun performSearch(query: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val searchField = findNodeByType(rootNode, "android.widget.EditText")
        if (searchField != null) {
            searchField.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val bundle = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
            }
            searchField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            return true
        }
        return false
    }

    /** Build a textual description of the currently visible screen (spec #10, #34). */
    fun describeScreen(): String {
        val root = rootInActiveWindow ?: return "No screen information available."
        val sb = StringBuilder()
        sb.append("Package: ${root.packageName}\n")
        collectInfo(root, sb, 0)
        return if (sb.isEmpty()) "Screen appears empty." else sb.toString().take(4000)
    }

    private fun collectInfo(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        if (node == null || sb.length > 3500) return
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
        val text = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        val clickable = node.isClickable
        val editable = node.isEditable
        val info = when {
            editable -> "[input] ${desc.ifBlank { text }}"
            text.isNotBlank() -> text
            desc.isNotBlank() -> desc
            else -> ""
        }
        if (info.isNotBlank()) {
            sb.append("${"  ".repeat(depth.coerceAtMost(6))}$cls: $info")
            if (clickable) sb.append(" (tap)")
            sb.append("\n")
        }
        for (i in 0 until node.childCount) {
            collectInfo(node.getChild(i), sb, depth + 1)
        }
    }

    /** Find a clickable element by visible text and tap it (semantic-first automation). */
    fun findAndClickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return clickMatching(root, text.lowercase())
    }

    private fun clickMatching(node: AccessibilityNodeInfo?, target: String): Boolean {
        if (node == null) return false
        val t = (node.text?.toString() ?: node.contentDescription?.toString() ?: "").lowercase()
        if (t.contains(target) && (node.isClickable || node.isFocusable)) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            if (clickMatching(node.getChild(i), target)) return true
        }
        return false
    }

    private fun findNodeByType(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.className == className) return node
        for (i in 0 until node.childCount) {
            val result = findNodeByType(node.getChild(i), className)
            if (result != null) return result
        }
        return null
    }
}
