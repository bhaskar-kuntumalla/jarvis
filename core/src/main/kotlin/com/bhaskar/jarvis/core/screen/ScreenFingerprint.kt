package com.bhaskar.jarvis.core.screen

import java.security.MessageDigest

/**
 * A SHA-256 digest of the visible text in a screen tree. It is used only to
 * detect whether a screen changed after an action. The digest is compared in
 * memory and never logged, so raw screen text is not retained.
 */
object ScreenFingerprint {

    fun of(root: ScreenNode?): String {
        if (root == null) return "no-screen"
        val parts = mutableListOf<String>()
        root.walk { node, _ ->
            if (node.visibleToUser) {
                parts += "${node.text.orEmpty()}\u001F${node.contentDescription.orEmpty()}"
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("\u001E").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun changed(before: ScreenNode?, after: ScreenNode?): Boolean =
        of(before) != of(after)
}
