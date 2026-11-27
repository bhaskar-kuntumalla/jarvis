package com.bhaskar.jarvis.core

/**
 * Parses one line of user input into a [Command].
 *
 * Grammar (verb is case-insensitive, arguments are plain text):
 *   open <app name>
 *   tap <visible text>
 *   type <text>
 *
 * An argument wrapped in matching double or single quotes is unwrapped, so
 * `tap "Wi-Fi"` targets `Wi-Fi`. Quotes that do not wrap the whole argument are
 * kept literally. Input is never interpreted by a shell.
 */
object CommandParser {

    fun parse(raw: String): ParseResult {
        val input = raw.trim()
        if (input.isEmpty()) return ParseResult.Rejected(CommandError.EMPTY_INPUT)

        val verbEnd = input.indexOfFirst { it.isWhitespace() }
        val verb = if (verbEnd == -1) input else input.substring(0, verbEnd)
        val argument = if (verbEnd == -1) "" else unwrapQuotes(input.substring(verbEnd).trim())

        val command = when (verb.lowercase()) {
            "open" -> Command.OpenApp(argument)
            "tap" -> Command.TapText(argument)
            "type" -> Command.TypeText(argument)
            else -> return ParseResult.Rejected(CommandError.UNKNOWN_VERB)
        }

        // Empty arguments are reported by the validator as MISSING_TARGET.
        return CommandValidator.validate(command)
            ?.let { ParseResult.Rejected(it) }
            ?: ParseResult.Ok(command)
    }

    private fun unwrapQuotes(value: String): String {
        if (value.length < 2) return value
        val quote = value.first()
        if ((quote != '"' && quote != '\'') || value.last() != quote) return value
        val inner = value.substring(1, value.length - 1)
        return if (inner.contains(quote)) value else inner
    }
}
