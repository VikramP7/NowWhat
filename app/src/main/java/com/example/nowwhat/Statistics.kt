package com.example.nowwhat

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt


private const val SLEEP_MAX_BRIDGE_HOURS = 1 // max missing sleep hours to assume same sleep episode
private const val SLEEP_MIN_EPISODE_HOURS = 3 // min number of hours required to count as a sleep episode
private const val SLEEP_PIVOT_HOUR = 12 // the point bedtimes are averaged from (assuming nobody sleeps across midday, and a night-shift user would want a midnight pivot)

enum class StatsWindow(val days: Int?) {
    ALL(null), WEEK(7), MONTH(30), QUARTER(90)
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

// Re-sorts totals into the stable activities-table order, so a colour keeps its place across
// columns, filters and legends. Totals whose activity isn't in the list are dropped.
fun List<ActivityTotal>.inOrderOf(activities: List<Activity>): List<ActivityTotal> {
    val byId = associateBy { it.activity.id }
    return activities.mapNotNull { byId[it.id] }
}

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
    val plannedSleepAverage: SleepAverage?,
    val nightlySleepAverages: Map<DayOfWeek, SleepAverage?>,
    val nightlyPlannedSleepAverages: Map<DayOfWeek, SleepAverage?>,

    val dayRhythm: List<RhythmColumn>,

    val weeklyShares: List<WeekShare>,

    val aspirationGap: List<ActivityGap>,

    val transitionMatrix: ActivityMatrix,
    val transitionChangesMatrix: ActivityMatrix
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
    val cutoff = truncateToHour(now)
    fun filterEntriesUntilNow(days: List<LocalDate>): List<HourEntry>{
        val daySet = days.toSet()
        return entries.filter { // contains logged, blank, and orphaned entries
            (it.timestamp < cutoff) && (logicalDateOf(it.timestamp, dayStartHour) in daySet)
        }
    }

    val loggedEntries = entries.filter { entry ->
        entry.actualActivityId != null
    }

    val firstLoggedDay = loggedEntries.minOfOrNull { logicalDateOf(it.timestamp, dayStartHour) }

    val filteredDays = daysInRange(
        filter = filter,
        firstLoggedDay = firstLoggedDay,
        today = today
    )

    val finishedHoursToday = logicalHourOf(hourOfDay(now), dayStartHour)
    val inRangeEntries = filterEntriesUntilNow(filteredDays)

    // -------- STATS CARDS (total days/hours) --------
    fun calendarHoursOf(days: List<LocalDate>): List<Long>{
        return days.flatMap { day -> List(if (day == today) finishedHoursToday else HOURS_IN_DAY) { logicalHour ->
                timestampOf(day, clockHourOf(logicalHour, dayStartHour), dayStartHour)
            }
        }
    }

    // ignores DST changes assumes 24-hour days
    fun hoursIn(days: List<LocalDate>): Int {
        return calendarHoursOf(days).size
    }

    val daysTracked = inRangeEntries.filter { activityMap[it.actualActivityId] != null }.distinctBy { logicalDateOf(it.timestamp, dayStartHour) }.size
    val hoursInRange = hoursIn(filteredDays)
    val calendarHours = calendarHoursOf(filteredDays)

    // -------- DONUT CHART VALUES --------
    val actual = totalsFor(inRangeEntries.map { it.actualActivityId }, activityMap)
    val planned = totalsFor(inRangeEntries.map { it.plannedActivityId }, activityMap)

    val blankLogHours = hoursInRange-(actual.enteredHours+actual.orphanedHours)
    val blankPlanHours = hoursInRange-(planned.enteredHours+planned.orphanedHours)
    val coverage = if (hoursInRange > 0) actual.enteredHours.toFloat()/hoursInRange.toFloat() else null

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

    fun episodesFor(plannedSleep: Boolean): List<SleepEpisode> {
        return if (sleepActivity != null){
            findSleepEpisodes(
                entries = entries,
                sleepActivityId = sleepActivity.id,
                dayStartHour = dayStartHour,
                windowNightSet = windowNights,
                plannedSleep = plannedSleep
            )
        }else{
            emptyList()
        }
    }
    val sleepEpisodes: List<SleepEpisode> = episodesFor(plannedSleep = false)

    val plannedSleepEpisodes: List<SleepEpisode> = episodesFor(plannedSleep = true)

    val filteredSleepAverage = averageSleep(sleepEpisodes.filter { it.nightOf.dayOfWeek in filter.dayType.nightList })
    val filteredPlannedSleepAverage = averageSleep(plannedSleepEpisodes.filter { it.nightOf.dayOfWeek in filter.dayType.nightList })

    // sort into day type
    val sleepDaysOfWeekAverages = DayOfWeek.entries.associateWith { day -> averageSleep(sleepEpisodes.filter { it.nightOf.dayOfWeek == day }) }
    val plannedSleepDaysOfWeekAverages = DayOfWeek.entries.associateWith { day -> averageSleep(plannedSleepEpisodes.filter { it.nightOf.dayOfWeek == day }) }


    // -------- DAY RHYTHM --------
    fun partitionOf(ids: List<Long?>, calendarHours: Int): HourPartition {
        val sideTotals = totalsFor(ids, activityMap)
        val orderedTotals = sideTotals.totals.inOrderOf(activities)
        return HourPartition(
            totals = orderedTotals,
            orphanedHours = sideTotals.orphanedHours,
            calendarHours = calendarHours
        )
    }

    val rhythmColumns = List(HOURS_IN_DAY) { logicalHour ->
        val clockHour = clockHourOf(logicalHour,dayStartHour)
        val col = inRangeEntries.filter { entry -> clockHour == hourOfDay(entry.timestamp) }
        val rhythmHourPartition = partitionOf(
            ids = col.map { it.actualActivityId },
            calendarHours = calendarHours.count { hourOfDay(it) == clockHour }
        )
        RhythmColumn(
            clockHour = clockHour,
            partition = rhythmHourPartition
        )
    }

    // -------- WEEKLY SHARES TIMELINE --------
    val timelineDays = daysInRange(
        filter = filter.copy(window = StatsWindow.ALL),
        firstLoggedDay = firstLoggedDay,
        today = today,
    )

    val timelineEntries = filterEntriesUntilNow(timelineDays)

    val entriesWeekGroups = timelineEntries.groupBy { weekStartOf(logicalDateOf(it.timestamp, dayStartHour)) }
    val listOfWeeksOfEntries = if (firstLoggedDay == null) emptyList() else {
        generateSequence(weekStartOf(firstLoggedDay)) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(weekStartOf(today))}
            .associateWith{entriesWeekGroups[it].orEmpty()}
        .toList()
    }

    val daysByWeek = timelineDays.groupBy { weekStartOf(it) }

    val weeklyShares = listOfWeeksOfEntries.map { (weekStart, weekEntries) ->
        WeekShare(
            weekStart = weekStart,
            partition = partitionOf(
                ids = weekEntries.map { it.actualActivityId },
                calendarHours = hoursIn(daysByWeek[weekStart].orEmpty())
            )
        )
    }

    // -------- ASPIRATION GAP --------
    val aspirationGap = aspirationGap(
        planned = planned.totals,
        actual = actual.totals,
        activities = activities
    )

    // -------- TRANSITION MATRIX --------
    val calendarHoursSet = calendarHours.toSet()
    val loggedTimestampLookup = inRangeEntries.associate { entry ->
        entry.timestamp to entry.actualActivityId?.takeIf { it in activityMap }
    }

    val transitionPairs = calendarHoursSet.mapNotNull { hourTimeStamp ->
        if (calendarHoursSet.contains(hourTimeStamp + HOUR_MS)){
            loggedTimestampLookup[hourTimeStamp] to loggedTimestampLookup[hourTimeStamp + HOUR_MS]
        }else {
            null
        }
    }

    val transitionMatrix = activityMatrix(
        pairs = transitionPairs,
        activities = activities
    )

    val transitionChangesMatrix = activityMatrix(
        pairs = transitionPairs.filter { pair -> pair.first != pair.second },
        activities = activities
    )


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
        plannedSleepAverage = filteredPlannedSleepAverage,
        nightlySleepAverages = sleepDaysOfWeekAverages,
        nightlyPlannedSleepAverages = plannedSleepDaysOfWeekAverages,
        dayRhythm = rhythmColumns,
        weeklyShares = weeklyShares,
        aspirationGap = aspirationGap,
        transitionMatrix = transitionMatrix,
        transitionChangesMatrix = transitionChangesMatrix
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
    val axis: List<Activity?>,      // same list, same order, for rows and columns
    val counts: List<List<Int>>    // counts[row][col]
)

private fun activityMatrix(
    pairs: List<Pair<Long?, Long?>>,
    activities: List<Activity>
): ActivityMatrix {
    // Determine which pairs actually contain non-null and real activities
    val activityIdSet = activities.map { it.id }.toSet()
    fun known(id: Long?) = id == null || id in activityIdSet
    val usedPairs = pairs.filter { known(it.first) && known(it.second) }

    // filter out all activities that are not used in the pairs list
    val usedActivityIdSet = (usedPairs.map {it.first}+usedPairs.map {it.second}).toSet()
    val usedActivities = activities.filter { it.id in usedActivityIdSet }
    // add null to the axis if used
    val axis = if (usedPairs.any { pair -> pair.first == null || pair.second == null}){
        usedActivities + listOf(null)
    }else{
        usedActivities
    }

    // group pairs and count how many in each group
    val pairCountsMap = usedPairs.groupingBy { it }.eachCount()
    val pairCounts = List(axis.size){ row -> List(axis.size) {col ->
            pairCountsMap[axis[row]?.id to (axis[col]?.id)] ?: 0
        }
    }

    return ActivityMatrix(
        axis = axis,
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

private fun findSleepEpisodes(entries: List<HourEntry>, sleepActivityId: Long, dayStartHour:Int, windowNightSet: Set<LocalDate>, plannedSleep: Boolean = false): List<SleepEpisode>{


    fun closeRun(bed: Long, wake: Long) = SleepEpisode(bed, wake, logicalDateOf(bed, dayStartHour))

    // filter out non-sleep activities
    val sleepEntries = entries.filter { entry ->
        if (plannedSleep){
            (entry.plannedActivityId == sleepActivityId)
        }else {
            (entry.actualActivityId == sleepActivityId)
        }
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
    val bedMinuteOfDay: Int get() = sleepOffsetToMinuteOfDay(bedOffset)
    val wakeMinuteOfDay: Int get() = sleepOffsetToMinuteOfDay(wakeOffset)
}

private fun roundToNearestFive(value: Float):Int{
    return (value/5).roundToInt()*5
}
fun sleepOffsetToMinuteOfDay(offset: Float):Int{
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

// A bucket of hours split the way every chart on the page splits them. calendarHours comes
// from the calendar, never from the rows, so blankHours is whatever the rows didn't cover.
data class HourPartition(
    val totals: List<ActivityTotal>,   // activities order
    val orphanedHours: Int,
    val calendarHours: Int
) {
    val loggedHours: Int get() = totals.sumOf { it.hours }
    val blankHours: Int get() = calendarHours - loggedHours - orphanedHours
}

data class RhythmColumn(
    val clockHour: Int,
    val partition: HourPartition
)

data class WeekShare(
    val weekStart: LocalDate,      // the Monday this bucket starts on (a logical date)
    val partition: HourPartition
)

data class ActivityGap(
    val activity: Activity,
    val plannedHours: Int,
    val actualHours: Int
)

private fun aspirationGap(
    planned: List<ActivityTotal>,
    actual: List<ActivityTotal>,
    activities: List<Activity>
): List<ActivityGap>{
    val plannedById = planned.associateBy { it.activity.id }
    val actualById = actual.associateBy { it.activity.id }
    return activities.mapNotNull { activity ->
        val plannedHours = plannedById[activity.id]?.hours
        val actualHours = actualById[activity.id]?.hours
        if (plannedHours == null && actualHours == null) null
        else ActivityGap(activity, plannedHours ?: 0, actualHours ?: 0)
    }
}

