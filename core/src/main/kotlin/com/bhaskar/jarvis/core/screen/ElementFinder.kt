package com.bhaskar.jarvis.core.screen

import com.bhaskar.jarvis.core.text.normalizeLabel

sealed interface TapResolution {
    /** Exactly one enabled, clickable target was found. */
    data class Found(val target: ScreenNode) : TapResolution

    /** No visible element carries this label. */
    object NotFound : TapResolution

    /** The label is visible, but no enabled clickable element contains it. */
    data class NotClickable(val label: String) : TapResolution

    /** More than one distinct clickable target carries this label. Never guess. */
    data class Ambiguous(val labels: List<String>) : TapResolution
}

object ElementFinder {

    /**
     * Resolves a tap target by exact (normalized) text or content description.
     * A matching label that is not itself clickable resolves to its nearest
     * clickable ancestor, which is what a user would tap.
     */
    fun resolveTap(root: ScreenNode, label: String): TapResolution {
        val wanted = normalizeLabel(label) ?: return TapResolution.NotFound

        val targets = linkedMapOf<String, ScreenNode>()
        val targetLabels = linkedMapOf<String, String>()
        var unclickableLabel: String? = null

        root.walk { node, ancestors ->
            if (!node.visibleToUser) return@walk
            val matches = normalizeLabel(node.text) == wanted ||
                normalizeLabel(node.contentDescription) == wanted
            if (!matches) return@walk

            val displayLabel = node.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: node.contentDescription?.trim().orEmpty()
            val target = (listOf(node) + ancestors).firstOrNull { it.clickable }
            if (target == null || !target.enabled) {
                if (unclickableLabel == null) unclickableLabel = displayLabel
                return@walk
            }
            targets.putIfAbsent(target.id, target)
            targetLabels.putIfAbsent(target.id, displayLabel)
        }

        val unclickable = unclickableLabel
        return when {
            targets.size == 1 -> TapResolution.Found(targets.values.first())
            targets.size > 1 -> TapResolution.Ambiguous(targetLabels.values.distinct())
            unclickable != null -> TapResolution.NotClickable(unclickable)
            else -> TapResolution.NotFound
        }
    }
}
