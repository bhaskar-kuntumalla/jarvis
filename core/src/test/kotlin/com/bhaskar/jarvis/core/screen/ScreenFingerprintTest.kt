package com.bhaskar.jarvis.core.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenFingerprintTest {

    @Test
    fun identicalScreensAreNotChanged() {
        val a = screen(label("t", text = "Home"))
        val b = screen(label("t", text = "Home"))
        assertFalse(ScreenFingerprint.changed(a, b))
    }

    @Test
    fun differentVisibleTextIsChanged() {
        val a = screen(label("t", text = "Home"))
        val b = screen(label("t", text = "Settings"))
        assertTrue(ScreenFingerprint.changed(a, b))
    }

    @Test
    fun invisibleNodeDifferencesDoNotCount() {
        val a = screen(label("t", text = "Home"))
        val b = screen(label("t", text = "Home"), label("h", text = "Hidden", visible = false))
        assertFalse(ScreenFingerprint.changed(a, b))
    }

    @Test
    fun missingScreenCountsAsChanged() {
        assertTrue(ScreenFingerprint.changed(null, screen(label("t", text = "Home"))))
    }

    @Test
    fun fingerprintDoesNotContainRawText() {
        val fp = ScreenFingerprint.of(screen(label("t", text = "SecretBalance123")))
        assertFalse(fp.contains("SecretBalance123"))
        assertEquals(64, fp.length)
    }
}
