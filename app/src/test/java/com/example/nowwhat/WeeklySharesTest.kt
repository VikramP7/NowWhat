package com.example.nowwhat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

// Tests for the portion-of-time weekly buckets (chunk 8b).
//
// Plain JUnit 4 on your computer's JVM, like SleepStatisticsTest: no device, no emulator.
// It works because Statistics.kt has no Android imports and computeStatistics takes `now`
// as a parameter, so every test pins the clock.
//
// The dates sit away from any DST change in North America and Europe, so "24 hours a day"
// holds on your machine. (If you ever move these dates, check that's still true.)

private const val START = 6 // day-start hour for every test in this file

private val ACTIVITIES = listOf("Work", "Sleep", "Gym", "Social", "Dating")
    .mapIndexed { index, name -> Activity(name, 0, id = index + 1L) }
private const val WORK = 1L
private const val SLEEP = 2L
private const val GYM = 3L

// The example data from the JVM run: first log on Thursday 17 Sep 2026, then every day
// until `now`. Sleep 11pm–7am, work 9–5 on weekdays, gym at 6pm, everything else unlogged.
private val FIRST_DAY: LocalDate = LocalDate.of(2026, 9, 17) // a Thursday
private val SUNDAY_4_OCT: LocalDate = LocalDate.of(2026, 10, 4)
private val WEDNESDAY_30_SEP: LocalDate = LocalDate.of(2026, 9, 30)

// A clock time on a *logical* day, exactly as the app means it: at(sunday, 1) is the 1am
// after Sunday evening, which on the calendar is Monday.
private fun at(logicalDay: LocalDate, hour: Int, minute: Int = 0): Long =
    timestampOf(logicalDay, hour, START) + minute * 60_000L

private fun logged(logicalDay: LocalDate, hour: Int, activityId: Long) =
    HourEntry(at(logicalDay, hour), plannedActivityId = null, actualActivityId = activityId)

// Every hour from FIRST_DAY up to (not including) the hour `now` is in.
private fun exampleWeeks(now: Long): List<HourEntry> {
    val entries = mutableListOf<HourEntry>()
    var day = FIRST_DAY
    while (!day.isAfter(logicalDateOf(now, START))) {
        for (hour in 0 until HOURS_IN_DAY) {
            if (at(day, hour) >= truncateToHour(now)) continue
            val activity: Long? = when {
                hour >= 23 || hour < 7 -> SLEEP
                hour in 9 until 17 && day.dayOfWeek <= DayOfWeek.FRIDAY -> WORK
                hour == 18 -> GYM
                else -> null
            }
            if (activity != null) entries += logged(day, hour, activity)
        }
        day = day.plusDays(1)
    }
    return entries
}

private fun stats(entries: List<HourEntry>, now: Long, filter: StatsFilter = StatsFilter()) =
    computeStatistics(
        entries = entries,
        activities = ACTIVITIES,
        dayStartHour = START,
        sleepActivityId = SLEEP,
        filter = filter,
        now = now
    )

// Debugging aid: the same readout as the temporary card. Drop a call into any test while
// you're chasing a number, read it in the Run window, then take it out again.
@Suppress("unused")
private fun printWeeks(stats: Statistics) {
    stats.weeklyShares.forEach { week ->
        val p = week.partition
        val mix = p.totals.joinToString { "${it.activity.name} ${it.hours}" }
        println("${week.weekStart}: $mix · del ${p.orphanedHours} · blank ${p.blankHours}/${p.calendarHours}")
    }
}

class WeeklySharesTest {

    private val sundayAfternoon = at(SUNDAY_4_OCT, 14, 30)

    // Two counts of the same thing sliced different ways. The first fails if the week axis
    // drops or doubles a day (a missing first week, an off-by-one end); the second fails if
    // an entry lands in no week or two. These are the checks that would have caught both
    // 8b bugs: summed calendar hours came to 1248, summed logged hours to 5.
    @Test
    fun weeksCoverEveryDayAndEveryEntryExactlyOnce() {
        val s = stats(exampleWeeks(sundayAfternoon), sundayAfternoon)

        assertEquals(s.hoursInRange, s.weeklyShares.sumOf { it.partition.calendarHours })
        assertEquals(s.loggedHours, s.weeklyShares.sumOf { it.partition.loggedHours })
    }

    // The window chips must not move this chart. Under All, inRangeEntries and the timeline
    // happen to agree, so only a run with a narrower window can tell them apart.
    @Test
    fun ignoresTheWindow() {
        val entries = exampleWeeks(sundayAfternoon)
        val all = stats(entries, sundayAfternoon, StatsFilter(window = StatsWindow.ALL))
        val week = stats(entries, sundayAfternoon, StatsFilter(window = StatsWindow.WEEK))

        assertEquals(all.weeklyShares, week.weeklyShares)
    }

    @Test
    fun weekStartsAreConsecutiveMondays() {
        val starts = stats(exampleWeeks(sundayAfternoon), sundayAfternoon).weeklyShares.map { it.weekStart }

        assertEquals(listOf(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 28)), starts)
        starts.forEach { assertEquals(DayOfWeek.MONDAY, it.dayOfWeek) }
    }

    // Hand-computed denominators: the first week only counts from the first log (Thu–Sun),
    // a full week is 168, and the current week is six days plus today's finished hours
    // (14:30 with a 6am start = 8 hours).
    @Test
    fun calendarHoursComeFromTheCalendarNotTheRows() {
        val weeks = stats(exampleWeeks(sundayAfternoon), sundayAfternoon).weeklyShares

        assertEquals(listOf(4 * 24, 7 * 24, 6 * 24 + 8), weeks.map { it.partition.calendarHours })
    }

    // The example data's hand-counted totals for the full middle week: 5 weekdays × 8 work
    // hours, 7 nights × 8 sleep hours, 7 gym hours, and nothing deleted.
    @Test
    fun fullWeekTotalsMatchTheExampleData() {
        val middle = stats(exampleWeeks(sundayAfternoon), sundayAfternoon).weeklyShares[1].partition

        assertEquals(listOf("Work" to 40, "Sleep" to 56, "Gym" to 7), middle.totals.map { it.activity.name to it.hours })
        assertEquals(0, middle.orphanedHours)
        assertEquals(168 - 40 - 56 - 7, middle.blankHours)
    }

    // 1am after Sunday evening is Monday on the calendar but Sunday's logical day, so it
    // belongs to the week that Sunday ends, not the next one.
    @Test
    fun smallHoursAfterSundayBelongToTheWeekBefore() {
        val sunday = LocalDate.of(2026, 9, 20)
        val now = at(LocalDate.of(2026, 9, 22), 12)
        val weeks = stats(listOf(logged(sunday, 1, SLEEP)), now).weeklyShares

        assertEquals(LocalDate.of(2026, 9, 14), weeks[0].weekStart)
        assertEquals(1, weeks[0].partition.loggedHours)
        assertEquals(0, weeks[1].partition.loggedHours)
    }

    // Under Weekends on a Wednesday, this week's Saturday hasn't happened. The week keeps its
    // slot (so the chart's x positions don't shift) with zero calendar hours, which 8c draws
    // as a gap rather than a 0% point.
    @Test
    fun weekendsMidweekKeepsTheCurrentWeekWithZeroHours() {
        val wednesday = at(WEDNESDAY_30_SEP, 14, 30)
        val weeks = stats(exampleWeeks(wednesday), wednesday, StatsFilter(dayType = DayType.WEEKENDS)).weeklyShares

        assertEquals(3, weeks.size)
        assertEquals(0, weeks.last().partition.calendarHours)
        assertTrue("no negative blanks", weeks.all { it.partition.blankHours >= 0 })
    }

    @Test
    fun nothingLoggedMeansNoWeeks() {
        assertEquals(emptyList<WeekShare>(), stats(emptyList(), sundayAfternoon).weeklyShares)
    }
}
