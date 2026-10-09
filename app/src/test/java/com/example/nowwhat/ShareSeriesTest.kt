package com.example.nowwhat

import androidx.compose.ui.graphics.Color
import com.example.nowwhat.ui.theme.OrphanedColour
import com.example.nowwhat.ui.theme.UnloggedColour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Tests for shareSeries: the step between the weekly numbers and the line chart.
// WeeklySharesTest checks the numbers; nothing checked this step, which is how an empty
// chart got past a fully green test run. Built from hand-made weeks rather than
// computeStatistics, so a failure here can only be shareSeries' fault.

private val WORK = Activity("Work", 0xFF2244AA.toInt(), id = 1)
private val SLEEP = Activity("Sleep", 0xFF66AADD.toInt(), id = 2)
private val GYM = Activity("Gym", 0xFFDD6622.toInt(), id = 3) // never logged below
private val ACTIVITIES = listOf(WORK, SLEEP, GYM)

private fun week(start: String, calendarHours: Int, orphaned: Int, vararg totals: Pair<Activity, Int>) =
    WeekShare(
        weekStart = LocalDate.parse(start),
        partition = HourPartition(totals.map { (activity, hours) -> ActivityTotal(activity, hours) }, orphaned, calendarHours)
    )

// Week 1: Work and Sleep. Week 2: no Work at all, two deleted-activity hours.
// Week 3: Weekends on a Wednesday, no hours had happened yet.
private val WEEKS = listOf(
    week("2026-09-14", calendarHours = 168, orphaned = 0, WORK to 40, SLEEP to 56),
    week("2026-09-21", calendarHours = 168, orphaned = 2, SLEEP to 50),
    week("2026-09-28", calendarHours = 0, orphaned = 0)
)

class ShareSeriesTest {

    private val lines = shareSeries(WEEKS, ACTIVITIES)
    private fun line(label: String) = lines.first { it.label == label }

    // The chart places points by index, so a line one value short slides every later
    // point one week to the left.
    @Test
    fun everyLineHasOneValuePerWeek() {
        // Without this first line the check below passes on an empty list: forEach over
        // nothing asserts nothing (a test that can't fail).
        assertTrue("there should be lines to check", lines.isNotEmpty())
        lines.forEach { assertEquals(it.label, WEEKS.size, it.values.size) }
    }

    @Test
    fun activitiesInTableOrderThenDeletedThenUnlogged() {
        assertEquals(listOf("Work", "Sleep", "Deleted Activities", "Unlogged"), lines.map { it.label })
        assertEquals(Color(WORK.colour), line("Work").colour)
        assertEquals(OrphanedColour, line("Deleted Activities").colour)
        assertEquals(UnloggedColour, line("Unlogged").colour)
    }

    @Test
    fun missingFromAWeekIsZeroButAWeekWithNoHoursIsAGap() {
        assertEquals(0f, line("Work").values[1]!!, 0f)
        lines.forEach { assertEquals(it.label, null, it.values[2]) }
    }

    // The cross-check: every hour of a week is one of the lines, so at each x the lines add
    // up to the whole week.
    @Test
    fun eachWeeksLinesAddUpToTheWholeWeek() {
        for (index in 0..1) {
            assertEquals(1f, lines.sumOf { it.values[index]!!.toDouble() }.toFloat(), 1e-5f)
        }
    }

    @Test
    fun hoursAreSharesOfThatWeeksOwnHours() {
        assertEquals(40f / 168, line("Work").values[0]!!, 1e-6f)
        assertEquals(50f / 168, line("Sleep").values[1]!!, 1e-6f)
    }

    @Test
    fun nothingLoggedMeansNoLines() {
        assertTrue(shareSeries(emptyList(), ACTIVITIES).isEmpty())
    }
}
