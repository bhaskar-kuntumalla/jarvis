package com.bhaskar.jarvis.core.apps

import com.bhaskar.jarvis.core.text.normalizeLabel

/** A launchable app as seen by the launcher. */
data class InstalledApp(val label: String, val packageName: String)

sealed interface AppMatch {
    data class Found(val app: InstalledApp) : AppMatch
    data class Ambiguous(val candidates: List<InstalledApp>) : AppMatch

    /** [suggestions] are shown to the user and are never launched automatically. */
    data class NotFound(val suggestions: List<String>) : AppMatch
}

object AppMatcher {
    private const val MAX_SUGGESTIONS = 5

    /**
     * Exact, case-insensitive label match. Exactly one matching package is
     * required to launch. Several packages with the same label are ambiguous.
     */
    fun match(query: String, apps: List<InstalledApp>): AppMatch {
        val wanted = normalizeLabel(query) ?: return AppMatch.NotFound(emptyList())

        val exact = apps
            .filter { normalizeLabel(it.label) == wanted }
            .distinctBy { it.packageName }

        return when (exact.size) {
            0 -> AppMatch.NotFound(
                apps.filter { normalizeLabel(it.label)?.contains(wanted) == true }
                    .map { it.label }
                    .distinct()
                    .take(MAX_SUGGESTIONS)
            )
            1 -> AppMatch.Found(exact.first())
            else -> AppMatch.Ambiguous(exact.sortedBy { it.packageName })
        }
    }
}
