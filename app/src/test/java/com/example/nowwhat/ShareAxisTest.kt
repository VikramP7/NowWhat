package com.example.nowwhat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for shareAxis, which fits the portion-of-time chart's y scale to its data.
//
// The example tests below pin the current tuning (MAX_GRID_DIVISIONS = 5), so retuning the
// constant should fail them: that's the test noticing a deliberate change, and the fix is to
// update the expected labels on purpose. The sweep test reads the constant itself, so it
// keeps checking the rules whatever the tuning.

class ShareAxisTest {

    private fun labels(axis: ShareAxis) = axis.gridLines.map { it.second }

    @Test
    fun topIsTheNextNiceStepAboveTheLargestShare() {
        val axis = shareAxis(0.37f)
        assertEquals(0.4f, axis.top, 1e-6f)
        assertEquals(listOf("0%", "10%", "20%", "30%", "40%"), labels(axis))
    }

    // The float trap: 0.3f × 100 is 30.000002, so without the nudge a share of exactly 30%
    // rounds up to 31% and the top jumps a whole step to 40%. 36 of a weekdays-only week's
    // 120 hours is that exact case, computed the way shareSeries computes it.
    @Test
    fun aShareExactlyOnAStepStaysOnIt() {
        assertEquals(0.3f, shareAxis(0.3f).top, 1e-6f)
        assertEquals(0.3f, shareAxis(36f / 120).top, 1e-6f)
        assertEquals(0.6f, shareAxis(0.6f).top, 1e-6f)
    }

    @Test
    fun aLargeShareUsesWiderSteps() {
        assertEquals(listOf("0%", "20%", "40%", "60%", "80%"), labels(shareAxis(0.62f)))
        assertEquals(listOf("0%", "20%", "40%", "60%", "80%", "100%"), labels(shareAxis(1f)))
    }

    // Every whole percent from 1 to 100: the top always covers the max, never by more than a
    // step, and the gridlines never exceed what MAX_GRID_DIVISIONS allows.
    @Test
    fun everyShareGetsATopThatFitsIt() {
        for (percent in 1..100) {
            val max = percent / 100f
            val axis = shareAxis(max)
            val step = axis.gridLines[1].first - axis.gridLines[0].first
            assertTrue("$percent%: top ${axis.top} below max", axis.top >= max - 1e-6f)
            assertTrue("$percent%: top ${axis.top} more than a step above", axis.top - max < step + 1e-6f)
            assertTrue("$percent%: ${axis.gridLines.size} gridlines", axis.gridLines.size <= MAX_GRID_DIVISIONS + 1)
            assertEquals("$percent%: last gridline is the top", axis.top, axis.gridLines.last().first)
        }
    }

    @Test
    fun nothingToFitStillGivesAUsableAxis() {
        val axis = shareAxis(0f)
        assertTrue(axis.top > 0f)
        assertEquals(listOf("0%", "5%"), labels(axis))
    }
}
