package com.example.nowwhat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SleepStatisticsTest {

    private val dayStart = 6
    private val sleep = Activity(name = "Sleep", colour = 0, id = 1)

    // June 2026: the 1st is a Monday. "Now" is Saturday the 13th at noon.
    private fun june(day: Int): LocalDate = LocalDate.of(2026, 6, day)
    private val now = timestampOf(june(13), 12, dayStart)

    // `hours` consecutive sleep hours, starting at `bedHour` on the evening of `night`.
    // Any offsets in `skip` are left out, to create gaps.
    private fun night(night: LocalDate, bedHour: Int, hours: Int, skip: Set<Int> = emptySet()) =
        (0 until hours).filter { it !in skip }.map { i ->
            HourEntry(
                timestamp = timestampOf(night, bedHour, dayStart) + i * HOUR_MS,
                plannedActivityId = null,
                actualActivityId = sleep.id
            )
        }

    private fun stats(entries: List<HourEntry>, filter: StatsFilter = StatsFilter()) =
        computeStatistics(
            entries = entries,
            activities = listOf(sleep),
            dayStartHour = dayStart,
            sleepActivityId = sleep.id,
            filter = filter,
            now = now
        )

    @Test
    fun bedtimesAverageAcrossMidnight() {
        // 11pm and 1am should average to midnight (offset 12), not noon.
        val avg = stats(night(june(1), 23, 8) + night(june(2), 1, 6)).sleepAverage!!
        assertEquals(12f, avg.bedOffset, 0.001f)
        assertEquals(7f, avg.hours, 0.001f)
        assertEquals(2, avg.nights)
    }

    @Test
    fun waketimeIsDerivedCorrectly() {
        // Waking at 5am and 7am should average to 6am (offset 18).
        val avg = stats(night(june(1), 23, 6) + night(june(2), 23, 8)).sleepAverage!!
        assertEquals(18f, avg.wakeOffset, 0.001f)
    }

    @Test
    fun earlyWakeStaysOnItsOwnNight() {
        // 10pm to 5am on Wednesday wakes before the 6am day start; it must not slip to Tuesday.
        val byNight = stats(night(june(3), 22, 7)).nightlySleepAverages
        assertEquals(1, byNight[DayOfWeek.WEDNESDAY]?.nights)
        assertNull(byNight[DayOfWeek.TUESDAY])
    }

    @Test
    fun singleGapIsBridgedAndNapsAreIgnored() {
        // 11pm to 7am with the 3am hour blank, plus a 2-hour afternoon nap.
        val avg = stats(night(june(4), 23, 8, skip = setOf(4)) + night(june(4), 14, 2)).sleepAverage!!
        assertEquals(1, avg.nights)
        assertEquals(8f, avg.hours, 0.001f)
    }

    @Test
    fun weekendsMeansFridayAndSaturdayNights() {
        val entries = night(june(5), 23, 8) + night(june(6), 23, 8) + night(june(7), 23, 5)
        val avg = stats(entries, StatsFilter(dayType = DayType.WEEKENDS)).sleepAverage!!
        assertEquals(2, avg.nights)
        assertEquals(8f, avg.hours, 0.001f)
    }

    @Test
    fun weekWindowIsTheLastSevenCompletedNights() {
        val week = StatsFilter(window = StatsWindow.WEEK)
        // Saturday the 6th is the oldest night in the window; Friday the 5th is just outside it.
        assertEquals(1, stats(night(june(6), 23, 8), week).sleepAverage?.nights)
        assertNull(stats(night(june(5), 23, 8), week).sleepAverage)
    }
}
