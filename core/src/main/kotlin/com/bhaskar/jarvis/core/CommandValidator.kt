package com.bhaskar.jarvis.core

/**
 * Policy checks applied after syntax parsing. Limits keep input bounded and
 * stop control characters from reaching accessibility or input APIs.
 */
object CommandValidator {
    const val MAX_TARGET_LENGTH = 100
    const val MAX_TEXT_LENGTH = 500

    /** Returns the first problem found, or null if the command is acceptable. */
    fun validate(command: Command): CommandError? = when (command) {
        is Command.OpenApp -> checkTarget(command.appName)
        is Command.TapText -> checkTarget(command.visibleText)
        is Command.TypeText -> checkText(command.text)
    }

    private fun checkTarget(value: String): CommandError? = when {
        value.isBlank() -> CommandError.MISSING_TARGET
        value.length > MAX_TARGET_LENGTH -> CommandError.TOO_LONG
        value.any { it.isISOControl() } -> CommandError.CONTROL_CHARACTER
        else -> null
    }

    private fun checkText(value: String): CommandError? = when {
        value.isEmpty() -> CommandError.MISSING_TARGET
        value.length > MAX_TEXT_LENGTH -> CommandError.TOO_LONG
        value.any { it.isISOControl() } -> CommandError.CONTROL_CHARACTER
        else -> null
    }
}
