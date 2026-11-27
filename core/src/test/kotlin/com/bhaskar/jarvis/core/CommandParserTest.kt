package com.bhaskar.jarvis.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParserTest {

    private fun ok(command: Command) = ParseResult.Ok(command)
    private fun rejected(error: CommandError) = ParseResult.Rejected(error)

    @Test
    fun parsesOpenWithAppName() {
        assertEquals(ok(Command.OpenApp("Chrome")), CommandParser.parse("open Chrome"))
    }

    @Test
    fun verbIsCaseInsensitiveAndWhitespaceTolerant() {
        assertEquals(ok(Command.OpenApp("Settings")), CommandParser.parse("  OPEN   Settings  "))
    }

    @Test
    fun parsesMultiWordAppName() {
        assertEquals(ok(Command.OpenApp("Google Play Store")), CommandParser.parse("open Google Play Store"))
    }

    @Test
    fun parsesTapWithUnquotedText() {
        assertEquals(ok(Command.TapText("Add to cart")), CommandParser.parse("tap Add to cart"))
    }

    @Test
    fun unwrapsDoubleQuotedTapTarget() {
        assertEquals(ok(Command.TapText("Wi-Fi")), CommandParser.parse("tap \"Wi-Fi\""))
    }

    @Test
    fun parsesTypeAndKeepsInternalSpacing() {
        assertEquals(ok(Command.TypeText("hello   world")), CommandParser.parse("type hello   world"))
    }

    @Test
    fun typeKeepsApostrophesInsideQuotes() {
        assertEquals(ok(Command.TypeText("it's fine")), CommandParser.parse("type \"it's fine\""))
    }

    @Test
    fun unbalancedQuotesAreKeptLiterally() {
        assertEquals(ok(Command.TypeText("\"hello")), CommandParser.parse("type \"hello"))
    }

    @Test
    fun shellLikeInputIsPlainText() {
        assertEquals(
            ok(Command.OpenApp("chrome; rm -rf /")),
            CommandParser.parse("open chrome; rm -rf /"),
        )
    }

    @Test
    fun emptyAndBlankInputAreRejected() {
        assertEquals(rejected(CommandError.EMPTY_INPUT), CommandParser.parse(""))
        assertEquals(rejected(CommandError.EMPTY_INPUT), CommandParser.parse("   \t  "))
    }

    @Test
    fun unknownVerbIsRejected() {
        assertEquals(rejected(CommandError.UNKNOWN_VERB), CommandParser.parse("launch Chrome"))
        assertEquals(rejected(CommandError.UNKNOWN_VERB), CommandParser.parse("openchrome"))
    }

    @Test
    fun verbWithoutTargetIsRejected() {
        assertEquals(rejected(CommandError.MISSING_TARGET), CommandParser.parse("open"))
        assertEquals(rejected(CommandError.MISSING_TARGET), CommandParser.parse("tap   "))
        assertEquals(rejected(CommandError.MISSING_TARGET), CommandParser.parse("type"))
    }

    @Test
    fun emptyQuotedArgumentIsRejected() {
        assertEquals(rejected(CommandError.MISSING_TARGET), CommandParser.parse("tap \"\""))
    }

    @Test
    fun controlCharactersAreRejected() {
        assertEquals(rejected(CommandError.CONTROL_CHARACTER), CommandParser.parse("type a\nb"))
    }

    @Test
    fun targetLengthBoundary() {
        val atLimit = "a".repeat(CommandValidator.MAX_TARGET_LENGTH)
        val overLimit = "a".repeat(CommandValidator.MAX_TARGET_LENGTH + 1)
        assertEquals(ok(Command.OpenApp(atLimit)), CommandParser.parse("open $atLimit"))
        assertEquals(rejected(CommandError.TOO_LONG), CommandParser.parse("open $overLimit"))
    }

    @Test
    fun textLengthBoundary() {
        val atLimit = "b".repeat(CommandValidator.MAX_TEXT_LENGTH)
        val overLimit = "b".repeat(CommandValidator.MAX_TEXT_LENGTH + 1)
        assertEquals(ok(Command.TypeText(atLimit)), CommandParser.parse("type $atLimit"))
        assertEquals(rejected(CommandError.TOO_LONG), CommandParser.parse("type $overLimit"))
    }
}
