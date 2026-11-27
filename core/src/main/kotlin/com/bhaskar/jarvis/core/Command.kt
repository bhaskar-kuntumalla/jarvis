package com.bhaskar.jarvis.core

/**
 * The complete allowlist of commands Milestone 1 can execute.
 * Anything else is rejected by the parser before it reaches a device.
 */
sealed interface Command {
    /** Launch an installed app by its user-visible name. */
    data class OpenApp(val appName: String) : Command

    /** Tap a visible control whose text or content description matches exactly. */
    data class TapText(val visibleText: String) : Command

    /** Type text into the currently focused editable field. */
    data class TypeText(val text: String) : Command
}

/** Why a command was rejected. [message] is safe to show the user. */
enum class CommandError(val message: String) {
    EMPTY_INPUT("Type a command, for example: open Chrome"),
    UNKNOWN_VERB("Unknown command. Use: open <app>, tap <text>, or type <text>"),
    MISSING_TARGET("This command needs a target after the verb"),
    TOO_LONG("That is longer than the allowed limit"),
    CONTROL_CHARACTER("Commands cannot contain line breaks or control characters"),
}

sealed interface ParseResult {
    data class Ok(val command: Command) : ParseResult
    data class Rejected(val error: CommandError) : ParseResult
}
