package com.bhaskar.jarvis.core.screen

data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val isEmpty: Boolean get() = right <= left || bottom <= top
}

/**
 * Immutable copy of one accessibility node. The Android layer converts live
 * nodes into this form so all matching logic can be tested without a device.
 */
data class ScreenNode(
    val id: String,
    val packageName: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val clickable: Boolean,
    val editable: Boolean,
    val focused: Boolean,
    val password: Boolean,
    val enabled: Boolean,
    val visibleToUser: Boolean,
    val bounds: Bounds,
    val children: List<ScreenNode> = emptyList(),
)

/** Depth-first walk. [action] receives each node and its ancestors, nearest first. */
internal fun ScreenNode.walk(action: (node: ScreenNode, ancestors: List<ScreenNode>) -> Unit) {
    fun visit(node: ScreenNode, ancestors: List<ScreenNode>) {
        action(node, ancestors)
        val nextAncestors = listOf(node) + ancestors
        node.children.forEach { visit(it, nextAncestors) }
    }
    visit(this, emptyList())
}
