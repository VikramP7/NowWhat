package com.example.nowwhat

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt


private const val SLEEP_MAX_BRIDGE_HOURS = 1 // max sleep missing sleep hours to assume same sleep episode
private const val SLEEP_MIN_EPISODE_HOURS = 3// min number of hours required to count as a sleep episode
private const val SLEEP_PIVOT_HOUR = 12 // the point bedtimes are averaged from (assuming nobody sleeps across midday, and a night-shift user would want a midnight pivot)

enum class StatsWindow(val days: Int?) {
    WEEK(7), MONTH(30), QUARTER(90), ALL(null)
}

enum class DayType(val dayList: List<DayOfWeek>, val nightList: List<DayOfWeek>) {
    ALL(
        DayOfWeek.entries,
        DayOfWeek.entries
    ),
    WEEKDAYS(
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)
    ),
    WEEKENDS(
        listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
    )
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
    val unpairedHours: Int,

    val sleepActivity: Activity?,
    val sleepAverage: SleepAverage?,
    val nightlySleepAverages: Map<DayOfWeek, SleepAverage?>
)

fun computeStatistics(
    entries: List<HourEntry>,
    activities: List<Activity>,
    dayStartHour: Int,
    sleepActivityId: Long?,
    filter: StatsFilter = StatsFilter(),
    now: Long = System.currentTimeMillis()
): Statistics
{
    // Build a lookup map by activityId to Activity object
    val activityMap: Map<Long, Activity> = activities.associateBy { it.id }
    val today = logicalDateOf(now, dayStartHour)


    // -------- APPLY FILTERING --------
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

    // -------- STATS CARDS (total days/hours) --------
    val daysTracked = inRangeEntries.filter { activityMap[it.actualActivityId] != null }.distinctBy { logicalDateOf(it.timestamp, dayStartHour) }.size

    val hoursInRange = filteredDays.sumOf { date ->
        if (date == today) wrapRange(hourOfDay(now) - dayStartHour) else 24
    }

    // -------- DONUT CHART VALUES --------
    val actual = totalsFor(inRangeEntries.map { it.actualActivityId }, activityMap)
    val planned = totalsFor(inRangeEntries.map { it.plannedActivityId }, activityMap)

    val blankLogHours = hoursInRange-(actual.enteredHours+actual.orphanedHours)
    val blankPlanHours = hoursInRange-(planned.enteredHours+planned.orphanedHours)
    val coverage = if (hoursInRange.toFloat() > 0) actual.enteredHours.toFloat()/hoursInRange.toFloat() else null

    // -------- PLAN VS ACTUAL CONFUSION MATRIX --------
    val planVsActualPairs = inRangeEntries.mapNotNull { entry ->
        val plannedId = entry.plannedActivityId
        val actualId = entry.actualActivityId
        if (plannedId != null && actualId != null) plannedId to actualId else null
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

    // -------- SLEEP STATS --------
    val sleepActivity = activityMap[sleepActivityId]
    val allDayTypeFilter = filter.copy(dayType = DayType.ALL)
    val windowNights = daysInRange(
        filter = allDayTypeFilter,
        firstLoggedDay = firstLoggedDay,
        today = today
    ).map { it.minusDays(1) }.toSet()

    val sleepEpisodes: List<SleepEpisode> = if (sleepActivity != null){
        findSleepEpisodes(
            entries = entries,
            sleepActivityId = sleepActivity.id,
            dayStartHour = dayStartHour,
            windowNightSet = windowNights
        )
    }else{
        emptyList()
    }

    val filteredSleepAverage = averageSleep(sleepEpisodes.filter { it.nightOf.dayOfWeek in filter.dayType.nightList })

    // sort into day type
    val sleepDaysOfWeekAverages = DayOfWeek.entries.associateWith { day -> averageSleep(sleepEpisodes.filter { it.nightOf.dayOfWeek == day }) }


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
        unpairedHours = unpairedHours,
        sleepActivity = sleepActivity,
        sleepAverage = filteredSleepAverage,
        nightlySleepAverages = sleepDaysOfWeekAverages,
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

data class SleepEpisode(
    val bedTime: Long, // the hour you are first asleep for
    val wakeTime: Long, // the hour you are first awake for
    val nightOf: LocalDate // the night of this day
){
    val hours: Int get() = ((wakeTime-bedTime)/HOUR_MS).toInt()
}

private fun findSleepEpisodes(entries: List<HourEntry>, sleepActivityId: Long, dayStartHour:Int, windowNightSet: Set<LocalDate>): List<SleepEpisode>{


    fun closeRun(bed: Long, wake: Long) = SleepEpisode(bed, wake, logicalDateOf(bed, dayStartHour))

    // filter out non-sleep activities
    val sleepEntries = entries.filter { entry ->
        (entry.actualActivityId == sleepActivityId)
    }.sortedBy { entry -> entry.timestamp } // sorted by chronological order oldest to newest entries

    val sleepEpisodes: MutableList<SleepEpisode> = mutableListOf()
    var curBedTime: Long? = null
    var curWakeTime: Long? = null
    // find runs and label by start date
    sleepEntries.forEach { entry ->
        if (curBedTime == null || curWakeTime == null){
            // sleep not started yet
            curBedTime = entry.timestamp
            curWakeTime = entry.timestamp + HOUR_MS
        }
        else if (entry.timestamp - curWakeTime <= SLEEP_MAX_BRIDGE_HOURS * HOUR_MS){
            // there is a sleep episode started, check to see if this sleep is part of it
            curWakeTime = entry.timestamp + HOUR_MS
        }
        else{
            // the sleep is over, add episode
            sleepEpisodes.add(closeRun(curBedTime, curWakeTime))

            // reset
            curBedTime = entry.timestamp
            curWakeTime = entry.timestamp + HOUR_MS
        }
    }

    if (curBedTime != null && curWakeTime != null){
        sleepEpisodes.add(closeRun(curBedTime, curWakeTime))
    }

    val inRangeSleepEpisodes = sleepEpisodes.filter { sleepEpisode ->
        (sleepEpisode.hours >= SLEEP_MIN_EPISODE_HOURS) && (sleepEpisode.nightOf in windowNightSet)
    }

    val perNightSleepEpisodes = inRangeSleepEpisodes.groupBy { sleepEpisode -> sleepEpisode.nightOf }
        .map {nightGroup -> nightGroup.value.maxBy { episode -> episode.hours } }

    return perNightSleepEpisodes
}

data class SleepAverage(
    val bedOffset: Float,
    val hours: Float,
    val nights: Int
){
    val wakeOffset: Float get() = bedOffset+hours
    val bedMinuteOfDay: Int = offsetToRoundedMinutes(bedOffset)
    val wakeMinuteOfDay: Int = offsetToRoundedMinutes(wakeOffset)
}

private fun roundToNearestFive(value: Float):Int{
    return (value/5).roundToInt()*5
}
private fun offsetToRoundedMinutes(offset: Float):Int{
    val minutes = roundToNearestFive((offset+SLEEP_PIVOT_HOUR)*60)
    return wrapRange(minutes, top = 24 * 60 - 1)
}

private fun averageSleep(episodes: List<SleepEpisode>): SleepAverage?{
    if (episodes.isEmpty()) return null

    val sumBedTime = episodes.sumOf { episode -> wrapRange(hourOfDay(episode.bedTime)-SLEEP_PIVOT_HOUR) }.toFloat()
    val sumHours = episodes.sumOf { episode -> episode.hours }.toFloat()
    val sleepAverage = SleepAverage(sumBedTime/episodes.size,sumHours/episodes.size,episodes.size)


    return sleepAverage
}