package com.bhaskar.jarvis.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SkeletonSmokeTest {
    @Test
    fun milestoneLabelIsSet() {
        assertEquals("M1-skeleton", SkeletonInfo.MILESTONE)
    }
}
