package com.example.nowwhat

import java.time.DayOfWeek
import java.time.LocalDate

enum class StatsWindow(val days: Int?) {
    WEEK(7), MONTH(30), QUARTER(90), ALL(null)
}

enum class DayType(val dayList: List<DayOfWeek>) {
    ALL(DayOfWeek.entries),
    WEEKDAYS(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
    WEEKENDS(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
}

data class StatsFilter(
    val window: StatsWindow = StatsWindow.ALL,
    val dayType: DayType = DayType.ALL
)

data class ActivityTotal(
    val activity: Activity,
    val hours: Int
)

data class Statistics(
    val loggedHours: Int,
    val blankLogHours: Int,
    val orphanedLogHours: Int,
    val plannedHours: Int,
    val blankPlanHours: Int,
    val orphanedPlanHours: Int,
    val hoursInRange: Int,

    val daysTracked: Int,
    val daysInRange: Int,

    val coverage: Float?,
    val plannedActivityTotals: List<ActivityTotal>,
    val actualActivityTotals: List<ActivityTotal>,

    val planVsActual: ActivityMatrix,
    val pairedHours: Int,
    val adherence: Float?,
    val unpairedHours: Int
)

fun computeStatistics(
    entries: List<HourEntry>,
    activities: List<Activity>,
    dayStartHour: Int,
    filter: StatsFilter = StatsFilter(),
    now: Long = System.currentTimeMillis()
): Statistics
{
    // Build a lookup map by activityId to Activity object
    val activityMap: Map<Long, Activity> = activities.associateBy { it.id }
    val today = logicalDateOf(now, dayStartHour)

    val loggedEntries = entries.filter { entry ->
        entry.actualActivityId != null
    }

    val firstLoggedDay = loggedEntries.minOfOrNull { logicalDateOf(it.timestamp, dayStartHour) }

    val filteredDays = daysInRange(
        filter = filter,
        firstLoggedDay = firstLoggedDay,
        today = today
    )

    val cutoff = truncateToHour(now)
    val daySet = filteredDays.toSet()
    val inRangeEntries = entries.filter {
        (it.timestamp < cutoff) && (logicalDateOf(it.timestamp, dayStartHour) in daySet)
    }

    val daysTracked = inRangeEntries.filter { activityMap[it.actualActivityId] != null }.distinctBy { logicalDateOf(it.timestamp, dayStartHour) }.size

    val actual = totalsFor(inRangeEntries.map { it.actualActivityId }, activityMap)
    val planned = totalsFor(inRangeEntries.map { it.plannedActivityId }, activityMap)

    val hoursInRange = filteredDays.sumOf { date ->
        if (date == today) wrapRange(hourOfDay(now) - dayStartHour) else 24
    }

    val blankLogHours = hoursInRange-(actual.enteredHours+actual.orphanedHours)
    val blankPlanHours = hoursInRange-(planned.enteredHours+planned.orphanedHours)
    val coverage = if (hoursInRange.toFloat() > 0) actual.enteredHours.toFloat()/hoursInRange.toFloat() else null

    val planVsActualPairs = inRangeEntries.mapNotNull { entry ->
        val planned = entry.plannedActivityId
        val actual = entry.actualActivityId
        if (planned != null && actual != null) planned to actual else null
    }

    val planVsActualMatrix = activityMatrix(
        pairs = planVsActualPairs,
        activities = activities
    )

    val pairedHours = planVsActualMatrix.counts.sumOf { it.sum() }
    val adherence =
        if (pairedHours == 0) null
        else planVsActualMatrix.counts.indices.sumOf { planVsActualMatrix.counts[it][it] } / pairedHours.toFloat()

    val unpairedHours = hoursInRange - pairedHours

    return Statistics(
        loggedHours = actual.enteredHours,
        blankLogHours = blankLogHours,
        orphanedLogHours = actual.orphanedHours,
        plannedHours = planned.enteredHours,
        blankPlanHours = blankPlanHours,
        orphanedPlanHours = planned.orphanedHours,
        hoursInRange = hoursInRange,
        daysTracked = daysTracked,
        daysInRange = filteredDays.size,
        coverage = coverage,
        plannedActivityTotals = planned.totals,
        actualActivityTotals = actual.totals,
        planVsActual = planVsActualMatrix,
        pairedHours = pairedHours,
        adherence = adherence,
        unpairedHours = unpairedHours
    )
}

private data class SideTotals(
    val totals: List<ActivityTotal>,
    val enteredHours: Int,
    val orphanedHours: Int
)

private fun totalsFor(ids: List<Long?>, activityMap: Map<Long, Activity>): SideTotals{

    val presentIds = ids.filterNotNull()
    val counts = presentIds.groupingBy  { it }.eachCount()

    val activityTotals = counts.mapNotNull { (id, count) -> activityMap[id]?.let { ActivityTotal(it, count) } }.sortedByDescending { it.hours }
    val totalEnteredHours = activityTotals.sumOf { it.hours }

    val totalOrphanedHours = presentIds.size - totalEnteredHours

    return SideTotals(
        totals = activityTotals,
        enteredHours = totalEnteredHours,
        orphanedHours = totalOrphanedHours
    )
}

fun daysInRange(
    filter: StatsFilter,
    firstLoggedDay: LocalDate?,
    today: LocalDate
): List<LocalDate>{
    val start =
        if(filter.window.days != null) {
            today.minusDays(filter.window.days - 1L)}
        else {
            firstLoggedDay ?: today
        }

    val days = generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(today)}
        .filter { (filter.dayType.dayList.contains(it.dayOfWeek)) }
        .toList()

    return days
}

data class ActivityMatrix(
    val axis: List<Activity>,      // same list, same order, for rows and columns
    val counts: List<List<Int>>    // counts[row][col]
)

private fun activityMatrix(
    pairs: List<Pair<Long, Long>>,
    activities: List<Activity>
): ActivityMatrix {
    // Determine which pairs actually contain non-null and real activities
    val activityIdSet = activities.map { it.id }.toSet()
    val usedPairs = pairs.filter { it.first in activityIdSet && it.second in activityIdSet }
    // filter out all activities that are not used in the pairs list
    val usedActivityIdSet = (usedPairs.map {it.first}+usedPairs.map {it.second}).toSet()
    val usedActivities = activities.filter { it.id in usedActivityIdSet }
    // group pairs and count how many in each group
    val pairCountsMap = usedPairs.groupingBy { it }.eachCount()
    val pairCounts = List(usedActivities.size){ row -> List(usedActivities.size) {col ->
            pairCountsMap[usedActivities[row].id to usedActivities[col].id] ?: 0
        }
    }

    return ActivityMatrix(
        axis = usedActivities,
        counts = pairCounts
    )
}

fun ActivityMatrix.rowFractions(): List<List<Float>> {
    return List(counts.size){ row ->
        val rowSum = counts[row].sum()
        List(counts.size) { col ->
            if (rowSum > 0) counts[row][col].toFloat()/rowSum.toFloat() else 0.0f
        }
    }
}