package com.bhaskar.jarvis.device

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bhaskar.jarvis.core.screen.Bounds
import com.bhaskar.jarvis.core.screen.ScreenNode

/**
 * Reads the active window and performs click and set-text actions on request.
 *
 * It does not react to accessibility events. Callers ask for a snapshot when
 * they need one, so nothing is recorded in the background.
 *
 * Node access must happen on the main thread.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /** Package of the app in the foreground, or null if no window is readable. */
    fun foregroundPackage(): String? {
        val root = rootInActiveWindow ?: return null
        val packageName = root.packageName?.toString()
        recycleNode(root)
        return packageName
    }

    /**
     * Copies the active window into an immutable tree. Ids are dot-separated
     * child indices from the root (for example "0.2.1"), used by [clickAt] and
     * [setTextAt] to find the same node again.
     */
    fun snapshot(): ScreenNode? {
        val root = rootInActiveWindow ?: return null
        return try {
            copyTree(root, ROOT_ID, Budget(MAX_NODES))
        } finally {
            recycleNode(root)
        }
    }

    /** Performs ACTION_CLICK on the node at [path]. Returns false if the node is gone. */
    fun clickAt(path: String): Boolean {
        val node = findLive(path) ?: return false
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } finally {
            recycleNode(node)
        }
    }

    /** Replaces the text of the editable node at [path]. Returns false if the node is gone. */
    fun setTextAt(path: String, text: String): Boolean {
        val node = findLive(path) ?: return false
        return try {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } finally {
            recycleNode(node)
        }
    }

    /** Returns the live node for [path]. The caller must recycle it. */
    private fun findLive(path: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val indices = path.split('.').drop(1).map { it.toIntOrNull() ?: return null }
        var current: AccessibilityNodeInfo? = root
        for (index in indices) {
            val parent = current ?: return null
            val next = parent.getChild(index)
            recycleNode(parent)
            current = next
        }
        return current
    }

    private fun copyTree(node: AccessibilityNodeInfo, id: String, budget: Budget): ScreenNode {
        budget.remaining--
        val rect = Rect().also { node.getBoundsInScreen(it) }
        val children = mutableListOf<ScreenNode>()
        for (index in 0 until node.childCount) {
            if (budget.remaining <= 0) break
            val child = node.getChild(index) ?: continue
            try {
                children += copyTree(child, "$id.$index", budget)
            } finally {
                recycleNode(child)
            }
        }
        return ScreenNode(
            id = id,
            packageName = node.packageName?.toString(),
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            clickable = node.isClickable,
            editable = node.supportsSetText(),
            focused = node.isFocused,
            password = node.isPassword,
            enabled = node.isEnabled,
            visibleToUser = node.isVisibleToUser,
            bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
            children = children,
        )
    }

    /**
     * Editable detection without AccessibilityNodeInfo.isEditable(), which needs
     * API 28. Nodes that accept ACTION_SET_TEXT are treated as editable.
     */
    private fun AccessibilityNodeInfo.supportsSetText(): Boolean =
        actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }

    @Suppress("DEPRECATION")
    private fun recycleNode(node: AccessibilityNodeInfo) {
        // recycle() is deprecated on API 33+ and is a no-op there. It is still
        // needed on API 26-32, so the call stays.
        node.recycle()
    }

    private class Budget(var remaining: Int)

    companion object {
        private const val ROOT_ID = "0"
        private const val MAX_NODES = 2000

        /** Set while the system has connected this service. Null otherwise. */
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set
    }
}
