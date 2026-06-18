package com.javis.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JavisAccessibilityService : AccessibilityService() {

    private var currentPackage: String = ""

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            notificationTimeout = 100
        }
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
