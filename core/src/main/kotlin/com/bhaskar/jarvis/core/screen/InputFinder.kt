package com.bhaskar.jarvis.core.screen

sealed interface InputResolution {
    /** Exactly one enabled, editable, focused, non-password field. */
    data class Found(val target: ScreenNode) : InputResolution

    /** Nothing on screen has input focus. */
    object NoFocusedField : InputResolution

    /** Something has focus, but it is not an enabled editable field. */
    object NotEditable : InputResolution

    /** The focused field is a password field. Milestone 1 never types into these. */
    object PasswordField : InputResolution

    /** More than one editable field reports focus. Never guess. */
    data class Ambiguous(val count: Int) : InputResolution
}

object InputFinder {

    fun resolveFocusedEditable(root: ScreenNode): InputResolution {
        val focused = mutableListOf<ScreenNode>()
        root.walk { node, _ -> if (node.focused) focused += node }

        val editable = focused.filter { it.editable && it.enabled }
        return when {
            editable.size > 1 -> InputResolution.Ambiguous(editable.size)
            editable.size == 1 -> {
                val field = editable.first()
                if (field.password) InputResolution.PasswordField else InputResolution.Found(field)
            }
            focused.isNotEmpty() -> InputResolution.NotEditable
            else -> InputResolution.NoFocusedField
        }
    }
}
