package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.OrphanedColour
import com.example.nowwhat.ui.theme.TextColour
import com.example.nowwhat.ui.theme.UnloggedColour
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun StatisticsSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val activities by viewModel.activities.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val filter by viewModel.statsFilter.collectAsState()
    val normalized by viewModel.statsNormalize.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundColour,
        topBar = {
            TopBarSettings(
                onClick = { onNavigate(AppScreenState.SETTINGS) },
                path = " > Statistics"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StatsChipRow(
                    options = StatsWindow.entries,
                    selected = filter.window,
                    label = {
                        when (it) {
                            StatsWindow.WEEK -> "Week"
                            StatsWindow.MONTH -> "Month"
                            StatsWindow.QUARTER -> "Quarter"
                            StatsWindow.ALL -> "All"
                        }
                    },
                    onSelect = viewModel::setStatsWindow
                )
            }
            item {
                StatsChipRow(
                    options = DayType.entries,
                    selected = filter.dayType,
                    label = {
                        when (it) {
                            DayType.ALL -> "All days"
                            DayType.WEEKDAYS -> "Weekdays"
                            DayType.WEEKENDS -> "Weekends"
                        }
                    },
                    onSelect = viewModel::setStatsDayType
                )
            }
            val stats = statistics
            if (stats == null) {
                item { Text(text = "Crunching the numbers...", color = TextColour) }
            } else {

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp))
                    {
                        HeadlineStat(
                            value = "${stats.loggedHours}",
                            label = "Hours Logged",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        HeadlineStat(
                            value = "${stats.daysTracked}/${stats.daysInRange}",
                            label = "Days Tracked",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        HeadlineStat(
                            value = if (stats.coverage != null)
                                "${(stats.coverage*100).roundToInt()} %" else "N/A",
                            label = "Coverage",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                val slices = stackSlices(stats.actualActivityTotals, stats.orphanedLogHours, stats.blankLogHours)

                item {
                    StatsCard(
                        title = "Activity Shares"
                    ) {
                        DonutChart(
                            slices = slices,
                            ringThickness = 110.dp,
                            centreContent = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${stats.loggedHours}",style = MaterialTheme.typography.displaySmall, color = TextColour)
                                    Text("of ${stats.hoursInRange} h",style = MaterialTheme.typography.titleMedium, color = TextColour)
                                }
                            }
                        )
                        DonutLegend( slices = slices )
                    }
                }

                item {
                    StatsCard(
                        title = "Plan vs Actual"
                    ) {
                        if (stats.adherence == null){
                            Text("No data in selected range...", color = TextColour)
                        } else{
                            Column(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${(stats.adherence * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextColour
                                )
                                Text(
                                    text = "of hours you logged went to plan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextColour
                                )
                            }
                            StatsChipRow(
                                options = listOf(true, false),
                                selected = normalized,
                                label = {
                                    when (it) {
                                        true -> "% of plan"
                                        false -> "Hours"
                                    }
                                },
                                onSelect = viewModel::setStatsNormalize
                            )
                            Text(
                                text = "Rows are planned, columns are actual",
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic,
                                color = TextColour
                            )
                            ActivityMatrixGrid(
                                matrix = stats.planVsActual,
                                normalised = normalized
                            )
                            ActivityLegend(stats.planVsActual.axis)
                            Text(
                                text = "${stats.unpairedHours} hours excluded (no plan or no log)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextColour
                            )
                        }
                    }
                } // END Plan vs Actual Matrix

                // -------- SLEEP --------
                val sleepAverage = stats.sleepAverage

                item {
                    StatsCard(
                        title = "Sleep"
                    ) {
                        if (stats.sleepActivity == null) {
                            Text("Choose which activity means sleep", color = TextColour)
                        }else{
                            Row {
                                Text("Sleep is tracked as ", color = TextColour)
                                Text(stats.sleepActivity.name, color = Color(stats.sleepActivity.colour))
                                Text(":", color = TextColour)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                            items(activities, key = { it.id }){ activity ->
                                PresetButton(
                                    activity = activity,
                                    onClick = {viewModel.setSleepActivityId(activity.id)},
                                    filled = stats.sleepActivity?.id == activity.id
                                )
                            }
                        }

                        if(stats.sleepActivity != null){
                            SleepBarChart(
                                nights = stats.nightlySleepAverages,
                                plannedNights = stats.nightlyPlannedSleepAverages,
                                average = stats.sleepAverage,
                                plannedAverage = stats.plannedSleepAverage,
                                barColour = Color(stats.sleepActivity.colour),
                                labelColour = TextColour,
                                is24Hour = is24Hour
                            )
                            Text(
                                text = "Each bar is a night, named for the evening it starts",
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic,
                                color = TextColour
                            )
                        }
                    }
                }

                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeadlineStat(
                            value = sleepAverage?.let { formatClockLabel(it.bedMinuteOfDay, is24Hour) } ?: "—",
                            label = "Avg Bedtime",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            valueStyle = MaterialTheme.typography.titleLarge
                        )
                        HeadlineStat(
                            value = sleepAverage?.let { formatDurationLabel(it.hours) } ?: "—",
                            label = "Avg Sleep",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            valueStyle = MaterialTheme.typography.titleLarge
                        )
                        HeadlineStat(
                            value = sleepAverage?.let { formatClockLabel(it.wakeMinuteOfDay, is24Hour) } ?: "—",
                            label = "Avg Waketime",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            valueStyle = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                item {
                    Text(
                        text = sleepCaption(
                            nights = sleepAverage?.nights,
                            dayType = filter.dayType
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontStyle = FontStyle.Italic,
                        color = TextColour,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // -------- DAY RHYTHM --------
                val rhythmColumns = stats.dayRhythm.map { stackSlices(it.partition.totals, it.partition.orphanedHours, it.partition.blankHours) }
                // Built by the same rule as the stacks, so the legend reads left to right in the
                // order the columns stack bottom to top.
                val rhythmLegend = stackSlices(
                    totals = stats.actualActivityTotals.inOrderOf(activities),
                    orphanedHours = stats.orphanedLogHours,
                    blankHours = stats.blankLogHours
                )
                // A label every 6 hours, on the same boundaries as the Morning/Day/Evening/Night
                // rows of the hours view. Index = logical hour, so column 0 is the day start.
                val rhythmLabels = stats.dayRhythm.mapIndexed { logicalHour, col ->
                    if (logicalHour % BAND_HOURS == 0) formatHourLabel(col.clockHour, is24Hour) else null
                }

                item {
                    StatsCard(title = "Day Rhythm") {
                        if (stats.hoursInRange == 0) {
                            Text("No hours in this range yet", color = TextColour)
                        } else {
                            StackedColumnChart(
                                columns = rhythmColumns,
                                labels = rhythmLabels,
                                labelColour = TextColour
                            )
                            SwatchLegend(entries = rhythmLegend.map { it.label to it.colour })
                            Text(
                                text = rhythmCaption(days = stats.daysInRange, dayType = filter.dayType),
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic,
                                color = TextColour
                            )
                        }
                    }
                }

                // -------- PORTION OF TIME --------
                // All time, whatever the window chip says; only the day-type chip applies here.
                val shareLines = shareSeries(stats.weeklyShares, activities)
                val shareLabels = monthLabels(stats.weeklyShares.map { it.weekStart })
                // Fitted to the largest share any line reaches, Unlogged included, so no line
                // is ever cut off at the top.
                val shareScale = shareAxis(shareLines.flatMap { it.values }.filterNotNull().maxOrNull() ?: 0f)

                item {
                    StatsCard(title = "Portion of Time") {
                        // No lines means no week had any hours yet (nothing logged, or a day-type
                        // filter whose days haven't come round since the first log).
                        if (shareLines.isEmpty()) {
                            Text("No hours to show yet", color = TextColour)
                        } else {
                            LineChart(
                                series = shareLines,
                                labelColour = TextColour,
                                xLabels = shareLabels,
                                gridLines = shareScale.gridLines,
                                yMax = shareScale.top
                            )
                            SwatchLegend(entries = shareLines.map { it.label to it.colour })
                            Text(
                                text = shareCaption(filter.dayType),
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic,
                                color = TextColour
                            )
                        }
                    }
                }
            }
        }
    }
}

// The caption under the three sleep cards. The night names come from DayType.nightList,
// the same list the average was filtered by, so the caption can't disagree with the numbers.
private fun sleepCaption(nights: Int?, dayType: DayType): String {
    if (nights == null) return "No complete nights in this range"

    val nightWord = if (nights == 1) "night" else "nights"
    return "Averaged over $nights $nightWord${dayNamesSuffix(dayType, dayType.nightList)}"
}

// The caption under the day rhythm. It names the *days* (dayList), where sleepCaption names the
// nights: the Weekends chip means Sat/Sun here but Fri/Sat nights on the sleep chart, and saying
// which is what stops the two from looking like they disagree.
private fun rhythmCaption(days: Int, dayType: DayType): String {
    val dayWord = if (days == 1) "day" else "days"
    return "Each column is one hour of the day, across $days $dayWord${dayNamesSuffix(dayType, dayType.dayList)}"
}

// The caption under portion of time. "Since your first log" is the part that matters: this
// chart ignores the window chips, and without saying so it looks broken when they don't move it.
private fun shareCaption(dayType: DayType): String =
    "Each week's share of its hours, since your first log${dayNamesSuffix(dayType, dayType.dayList)}"

// " · Fri, Sat", or nothing under All days. The one home for how every caption names the
// days it covers; each caption passes the list it was filtered by (days, or nights for sleep).
private fun dayNamesSuffix(dayType: DayType, days: List<DayOfWeek>): String =
    if (dayType == DayType.ALL) ""
    else " · " + days.joinToString(", ") { it.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()) }

// The y range for a chart of shares, fitted to the data: the top of the plot, and the
// gridlines from 0 up to it. The top is the first "nice" step at or above the largest value,
// with steps chosen so there are at most MAX_GRID_DIVISIONS gaps between gridlines.
internal data class ShareAxis(
    val top: Float,
    val gridLines: List<Pair<Float, String>>
)

private val NICE_PERCENT_STEPS = listOf(5, 10, 20, 25, 50)
private const val MAX_GRID_DIVISIONS = 5

internal fun shareAxis(maxShare: Float): ShareAxis {
    // Whole percents, so the step arithmetic is exact. The 0.01 nudge is for float error:
    // 0.3f × 100 is 30.000002, and without it a share of exactly 30% would round up to 31
    // and push the top a whole step higher.
    val maxPercent = ceil(maxShare * 100 - 0.01f).toInt().coerceIn(1, 100)

    // (a + b - 1) / b is integer division rounding up: how many steps it takes to reach the max.
    fun stepsToReach(step: Int) = (maxPercent + step - 1) / step

    val step = NICE_PERCENT_STEPS.first { stepsToReach(it) <= MAX_GRID_DIVISIONS }
    val divisions = stepsToReach(step)
    return ShareAxis(
        top = step * divisions / 100f,
        gridLines = (0..divisions).map { i -> i * step / 100f to "${i * step}%" }
    )
}

// A label under the first week that starts in each month: the month's short name, plus the
// year on the very first label and on every January, so a long history can still be placed.
// Null for every other week. The chart skips any label that would collide, so this doesn't
// need to know how wide the screen is.
private fun monthLabels(weekStarts: List<LocalDate>): List<String?> =
    weekStarts.mapIndexed { index, start ->
        val previous = weekStarts.getOrNull(index - 1)
        if (previous != null && previous.month == start.month) null
        else {
            val month = start.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
            if (previous == null || start.monthValue == 1) "$month ${start.year}" else month
        }
    }

// The two kinds of hour that aren't an activity. One home for their names, because every
// chart that shows them (donut, rhythm, portion of time) has to call them the same thing.
private const val DELETED_LABEL = "Deleted Activities"
private const val UNLOGGED_LABEL = "Unlogged"

private fun stackSlices(totals: List<ActivityTotal>, orphanedHours: Int, blankHours: Int): List<ChartSlice>{
    val slices: MutableList<ChartSlice> =
        totals.map {ChartSlice(it.activity.name, Color(it.activity.colour), it.hours)
        }.toMutableList()
    if (orphanedHours>0) slices.add(ChartSlice(DELETED_LABEL, OrphanedColour, orphanedHours ))
    if (blankHours>0) slices.add(ChartSlice(UNLOGGED_LABEL, UnloggedColour, blankHours ))
    return slices.toList()
}

// One line per kind of hour, one value per week. Every line is built by mapping over ALL the
// weeks, so each has exactly weeks.size values and index i is always week i, whatever the
// data contains. (internal rather than private so ShareSeriesTest can reach it.)
internal fun shareSeries(weeks: List<WeekShare>, activities: List<Activity>): List<ChartSeries> {

    // A week's share of some hours: those hours ÷ that week's own calendar hours.
    // Null when the week had no hours to share out, so the chart breaks the line there.
    fun line(label: String, colour: Color, hoursIn: (HourPartition) -> Int) = ChartSeries(
        label = label,
        colour = colour,
        values = weeks.map { week ->
            val p = week.partition
            if (p.calendarHours == 0) null else hoursIn(p).toFloat() / p.calendarHours
        }
    )

    // Activities with hours in any week, in the activities-table order.
    val present = activities.filter { activity ->
        weeks.any { week -> week.partition.totals.any { it.activity.id == activity.id } }
    }

    return buildList {
        present.forEach { activity ->
            // An activity missing from a week that had hours is a real 0%, not a gap.
            add(line(activity.name, Color(activity.colour)) { p ->
                p.totals.find { it.activity.id == activity.id }?.hours ?: 0
            })
        }
        if (weeks.any { it.partition.orphanedHours > 0 }) add(line(DELETED_LABEL, OrphanedColour) { it.orphanedHours })
        if (weeks.any { it.partition.blankHours > 0 }) add(line(UNLOGGED_LABEL, UnloggedColour) { it.blankHours })
    }
}
