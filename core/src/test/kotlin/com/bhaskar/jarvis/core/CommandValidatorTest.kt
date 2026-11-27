package com.bhaskar.jarvis.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandValidatorTest {

    @Test
    fun validCommandsPass() {
        assertNull(CommandValidator.validate(Command.OpenApp("Chrome")))
        assertNull(CommandValidator.validate(Command.TapText("Sign in")))
        assertNull(CommandValidator.validate(Command.TypeText("hello")))
    }

    @Test
    fun blankTargetsAreMissing() {
        assertEquals(CommandError.MISSING_TARGET, CommandValidator.validate(Command.OpenApp("   ")))
        assertEquals(CommandError.MISSING_TARGET, CommandValidator.validate(Command.TapText("")))
        assertEquals(CommandError.MISSING_TARGET, CommandValidator.validate(Command.TypeText("")))
    }

    @Test
    fun controlCharactersInEveryCommandAreRejected() {
        assertEquals(CommandError.CONTROL_CHARACTER, CommandValidator.validate(Command.OpenApp("a\tb")))
        assertEquals(CommandError.CONTROL_CHARACTER, CommandValidator.validate(Command.TapText("a\u0000b")))
        assertEquals(CommandError.CONTROL_CHARACTER, CommandValidator.validate(Command.TypeText("a\rb")))
    }

    @Test
    fun overlongTargetsAndTextAreRejected() {
        val longTarget = "x".repeat(CommandValidator.MAX_TARGET_LENGTH + 1)
        val longText = "x".repeat(CommandValidator.MAX_TEXT_LENGTH + 1)
        assertEquals(CommandError.TOO_LONG, CommandValidator.validate(Command.TapText(longTarget)))
        assertEquals(CommandError.TOO_LONG, CommandValidator.validate(Command.TypeText(longText)))
    }

    @Test
    fun everyErrorHasUserFacingMessage() {
        CommandError.values().forEach { assertTrue(it.message.isNotBlank()) }
    }
}
