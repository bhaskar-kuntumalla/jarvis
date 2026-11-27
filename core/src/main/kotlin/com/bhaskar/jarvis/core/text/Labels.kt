package com.bhaskar.jarvis.core.text

/**
 * Normalizes a visible label for comparison: trims, collapses whitespace, and
 * lowercases. Returns null for blank input. Matching is exact after this step;
 * there is deliberately no fuzzy or prefix matching.
 */
internal fun normalizeLabel(value: String?): String? =
    value?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()?.takeIf { it.isNotEmpty() }
