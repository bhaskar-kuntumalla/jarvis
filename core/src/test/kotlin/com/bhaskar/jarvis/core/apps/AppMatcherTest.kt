package com.bhaskar.jarvis.core.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMatcherTest {

    private val apps = listOf(
        InstalledApp("Chrome", "com.android.chrome"),
        InstalledApp("Settings", "com.android.settings"),
        InstalledApp("Calendar", "com.google.calendar"),
        InstalledApp("Calendar", "com.samsung.calendar"),
    )

    @Test
    fun exactMatchIgnoresCase() {
        assertEquals(
            AppMatch.Found(InstalledApp("Chrome", "com.android.chrome")),
            AppMatcher.match("chrome", apps),
        )
    }

    @Test
    fun twoPackagesWithSameLabelAreAmbiguous() {
        val result = AppMatcher.match("Calendar", apps)
        assertTrue(result is AppMatch.Ambiguous)
        assertEquals(2, (result as AppMatch.Ambiguous).candidates.size)
    }

    @Test
    fun partialNameIsNotGuessed() {
        val result = AppMatcher.match("Setting", apps)
        assertEquals(AppMatch.NotFound(listOf("Settings")), result)
    }

    @Test
    fun unknownAppIsNotFoundWithoutSuggestions() {
        assertEquals(AppMatch.NotFound(emptyList()), AppMatcher.match("Photoshop", apps))
    }

    @Test
    fun blankQueryIsNotFound() {
        assertEquals(AppMatch.NotFound(emptyList()), AppMatcher.match("   ", apps))
    }

    @Test
    fun duplicatePackageEntriesCollapse() {
        val dupes = listOf(
            InstalledApp("Maps", "com.google.maps"),
            InstalledApp("Maps", "com.google.maps"),
        )
        assertEquals(AppMatch.Found(InstalledApp("Maps", "com.google.maps")), AppMatcher.match("Maps", dupes))
    }
}
