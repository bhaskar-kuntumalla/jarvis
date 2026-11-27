package com.bhaskar.jarvis.core.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFinderTest {

    @Test
    fun focusedEditableFieldIsFound() {
        val root = screen(field("f1"))
        val result = InputFinder.resolveFocusedEditable(root)
        assertEquals("f1", (result as InputResolution.Found).target.id)
    }

    @Test
    fun noFocusMeansNoFocusedField() {
        val root = screen(field("f1", focused = false))
        assertEquals(InputResolution.NoFocusedField, InputFinder.resolveFocusedEditable(root))
    }

    @Test
    fun focusedNonEditableIsNotEditable() {
        val root = screen(label("b1", text = "Search", clickable = true).copy(focused = true))
        assertEquals(InputResolution.NotEditable, InputFinder.resolveFocusedEditable(root))
    }

    @Test
    fun focusedDisabledFieldIsNotEditable() {
        val root = screen(field("f1", enabled = false))
        assertEquals(InputResolution.NotEditable, InputFinder.resolveFocusedEditable(root))
    }

    @Test
    fun focusedPasswordFieldIsRefused() {
        val root = screen(field("p1", password = true))
        assertEquals(InputResolution.PasswordField, InputFinder.resolveFocusedEditable(root))
    }

    @Test
    fun twoFocusedEditableFieldsAreAmbiguous() {
        val root = screen(field("f1"), field("f2"))
        val result = InputFinder.resolveFocusedEditable(root)
        assertTrue(result is InputResolution.Ambiguous)
        assertEquals(2, (result as InputResolution.Ambiguous).count)
    }
}
