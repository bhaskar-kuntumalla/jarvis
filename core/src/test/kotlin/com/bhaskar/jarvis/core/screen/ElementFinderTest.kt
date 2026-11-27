package com.bhaskar.jarvis.core.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementFinderTest {

    @Test
    fun findsClickableButtonByText() {
        val root = screen(label("b1", text = "Sign in", clickable = true))
        val result = ElementFinder.resolveTap(root, "Sign in")
        assertTrue(result is TapResolution.Found)
        assertEquals("b1", (result as TapResolution.Found).target.id)
    }

    @Test
    fun matchesContentDescriptionWhenTextIsMissing() {
        val root = screen(label("i1", desc = "Back", clickable = true))
        val result = ElementFinder.resolveTap(root, "Back")
        assertEquals("i1", (result as TapResolution.Found).target.id)
    }

    @Test
    fun labelInsideClickableParentResolvesToParent() {
        val root = screen(
            label("row", clickable = true, children = listOf(label("txt", text = "Wi-Fi")))
        )
        val result = ElementFinder.resolveTap(root, "Wi-Fi")
        assertEquals("row", (result as TapResolution.Found).target.id)
    }

    @Test
    fun normalizesCaseAndWhitespace() {
        val root = screen(label("b1", text = "Wi-Fi", clickable = true))
        val result = ElementFinder.resolveTap(root, "  wi-FI ")
        assertEquals("b1", (result as TapResolution.Found).target.id)
    }

    @Test
    fun partialTextDoesNotMatch() {
        val root = screen(label("b1", text = "Wi-Fi", clickable = true))
        assertEquals(TapResolution.NotFound, ElementFinder.resolveTap(root, "Wi"))
    }

    @Test
    fun missingLabelIsNotFound() {
        val root = screen(label("b1", text = "Sign in", clickable = true))
        assertEquals(TapResolution.NotFound, ElementFinder.resolveTap(root, "Register"))
    }

    @Test
    fun invisibleNodesAreIgnored() {
        val root = screen(label("b1", text = "Sign in", clickable = true, visible = false))
        assertEquals(TapResolution.NotFound, ElementFinder.resolveTap(root, "Sign in"))
    }

    @Test
    fun visibleLabelWithoutClickableAncestorIsNotClickable() {
        val root = screen(label("t1", text = "Terms apply"))
        val result = ElementFinder.resolveTap(root, "Terms apply")
        assertEquals(TapResolution.NotClickable("Terms apply"), result)
    }

    @Test
    fun disabledButtonIsNotClickable() {
        val root = screen(label("b1", text = "Pay", clickable = true, enabled = false))
        assertEquals(TapResolution.NotClickable("Pay"), ElementFinder.resolveTap(root, "Pay"))
    }

    @Test
    fun twoDistinctButtonsWithSameLabelAreAmbiguous() {
        val root = screen(
            label("b1", text = "OK", clickable = true),
            label("b2", text = "OK", clickable = true),
        )
        val result = ElementFinder.resolveTap(root, "OK")
        assertEquals(TapResolution.Ambiguous(listOf("OK")), result)
    }

    @Test
    fun sameLabelTwiceInsideOneButtonIsStillOneTarget() {
        val root = screen(
            label("btn", clickable = true, children = listOf(
                label("t", text = "Share"),
                label("i", desc = "Share"),
            ))
        )
        val result = ElementFinder.resolveTap(root, "Share")
        assertEquals("btn", (result as TapResolution.Found).target.id)
    }
}
