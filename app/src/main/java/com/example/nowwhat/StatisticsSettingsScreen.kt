package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import java.time.format.TextStyle as DateTextStyle

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
    val changesOnly by viewModel.statsChangesOnly.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()

    var donutSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var shareSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var shareWeek by rememberSaveable { mutableStateOf<Int?>(null) }
    var sleepSelection by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }

    val selectDonutSlice: (String?) -> Unit = { label ->
        donutSelection = if (label == null || label == donutSelection) null else label
    }


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
        val fadeColour = BackgroundColour
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsChipRow(
                    options = StatsWindow.entries,
                    selected = filter.window,
                    label = {
                        when (it) {
                            StatsWindow.ALL -> "All"
                            StatsWindow.WEEK -> "Week"
                            StatsWindow.MONTH -> "Month"
                            StatsWindow.QUARTER -> "Quarter"
                        }
                    },
                    onSelect = viewModel::setStatsWindow
                )
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawWithContent {
                        drawContent()
                        val fadeHeight = 10.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(fadeColour, fadeColour.copy(alpha = 0f)),
                                startY = 0f,
                                endY = fadeHeight
                            ),
                            size = Size(size.width, fadeHeight)
                        )
                    },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stats = statistics
                if (stats == null) {
                    item { Text(text = "Crunching the numbers...", color = TextColour) }
                } else {

                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        )
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
                                    "${(stats.coverage * 100).roundToInt()} %" else "N/A",
                                label = "Coverage",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                    val slices = stackSlices(
                        stats.actualActivityTotals,
                        stats.orphanedLogHours,
                        stats.blankLogHours
                    )

                    item {
                        StatsCard(
                            title = "Where Your Time Went"
                        ) {
                            DonutChart(
                                slices = slices,
                                ringFraction = 0.32f,
                                selectedLabel = donutSelection,
                                onSliceClick = selectDonutSlice,
                                centreContent = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${stats.loggedHours}",
                                            style = MaterialTheme.typography.displaySmall,
                                            color = TextColour
                                        )
                                        Text(
                                            "of ${stats.hoursInRange} h",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextColour
                                        )
                                    }
                                }
                            )
                            DonutLegend(
                                slices = slices,
                                selectedLabel = donutSelection,
                                onRowClick = selectDonutSlice
                            )
                        }
                    }

                    // -------- PLAN VS ACTUAL --------
                    item {
                        StatsCard(
                            title = "Plan vs Actual"
                        ) {
                            if (stats.adherence == null) {
                                Text("No hours with both a plan and a log in this range", color = TextColour)
                            } else {
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
                                ActivityLegend(stats.planVsActual.axis.filterNotNull())
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
                            } else {
                                Row {
                                    Text("Sleep is tracked as ", color = TextColour)
                                    Text(
                                        stats.sleepActivity.name,
                                        color = Color(stats.sleepActivity.colour)
                                    )
                                    Text(":", color = TextColour)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(activities, key = { it.id }) { activity ->
                                    PresetButton(
                                        activity = activity,
                                        onClick = { viewModel.setSleepActivityId(activity.id) },
                                        filled = stats.sleepActivity?.id == activity.id
                                    )
                                }
                            }

                            if (stats.sleepActivity != null) {
                                SleepBarChart(
                                    nights = stats.nightlySleepAverages,
                                    plannedNights = stats.nightlyPlannedSleepAverages,
                                    average = stats.sleepAverage,
                                    plannedAverage = stats.plannedSleepAverage,
                                    barColour = Color(stats.sleepActivity.colour),
                                    labelColour = TextColour,
                                    is24Hour = is24Hour,
                                    selectedDay = sleepSelection,
                                    // The same night again, or the gutter, clears; another night selects it.
                                    onDayClick = { day -> sleepSelection = if (day == null || day == sleepSelection) null else day }
                                )
                                sleepSelection?.let { day ->
                                    Text(
                                        text = sleepReadout(day, stats.nightlySleepAverages[day], is24Hour),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextColour
                                    )
                                }
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
                                value = sleepAverage?.let {
                                    formatClockLabel(
                                        it.bedMinuteOfDay,
                                        is24Hour
                                    )
                                } ?: "—",
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
                                value = sleepAverage?.let {
                                    formatClockLabel(
                                        it.wakeMinuteOfDay,
                                        is24Hour
                                    )
                                } ?: "—",
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
                    val rhythmColumns = stats.dayRhythm.map {
                        stackSlices(
                            it.partition.totals,
                            it.partition.orphanedHours,
                            it.partition.blankHours
                        )
                    }
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
                        if (logicalHour % BAND_HOURS == 0) formatHourLabel(
                            col.clockHour,
                            is24Hour
                        ) else null
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
                                    text = rhythmCaption(
                                        days = stats.daysInRange,
                                        dayType = filter.dayType
                                    ),
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
                    val shareScale =
                        shareAxis(
                            shareLines.flatMap { it.values }.filterNotNull().maxOrNull() ?: 0f
                        )

                    item {
                        StatsCard(title = "Portion of Time") {
                            // No lines means no week had any hours yet (nothing logged, or a day-type
                            // filter whose days haven't come round since the first log).
                            if (shareLines.isEmpty()) {
                                Text("No hours to show yet", color = TextColour)
                            } else {
                                fun selectShare(label: String?, week: Int?) {
                                    shareSelection = label
                                    shareWeek = if (label == null) null else week
                                }

                                LineChart(
                                    series = shareLines,
                                    labelColour = TextColour,
                                    xLabels = shareLabels,
                                    gridLines = shareScale.gridLines,
                                    yMax = shareScale.top,
                                    visiblePoints = 12,
                                    selectedLabel = shareSelection,
                                    selectedIndex = shareWeek,
                                    // Near a line: select it at that week (tapping along the selected line moves the
                                    // readout). Away from every line: clear.
                                    onTap = { label, week -> selectShare(label, week) }
                                )

                                // The readout, only while a line is selected and still on the chart.
                                val selectedLine = shareLines.firstOrNull { it.label == shareSelection }
                                val readoutWeek = shareWeek
                                if (selectedLine != null && readoutWeek != null) {
                                    stats.weeklyShares.getOrNull(readoutWeek)?.let { week ->
                                        Text(
                                            text = shareReadout(selectedLine.label, selectedLine.values.getOrNull(readoutWeek), week.weekStart),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextColour
                                        )
                                    }
                                }

                                SwatchLegend(
                                    entries = shareLines.map { it.label to it.colour },
                                    selectedName = shareSelection,
                                    // The same entry again clears; another entry selects that line at its newest week
                                    // with a value, which is the end the chart opens on.
                                    onEntryClick = { label ->
                                        if (label == shareSelection) selectShare(null, null)
                                        else selectShare(
                                            label,
                                            shareLines.first { it.label == label }.values.indexOfLast { it != null }.takeIf { it >= 0 }
                                        )
                                    }
                                )
                                Text(
                                    text = shareCaption(filter.dayType),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = TextColour
                                )
                            }
                        }
                    }

                    // -------- ASPIRATION GRAPH --------
                    item {
                        StatsCard(title = "Aspiration Gap") {
                            if (stats.aspirationGap.isEmpty()) {
                                Text(
                                    text = "Nothing planned or logged in this range",
                                    color = TextColour
                                )
                            } else {
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
                                PairedBarChart(bars = gapBars(stats.aspirationGap, normalized))
                                PairedBarKey(ghostLabel = "Planned", solidLabel = "Actual")
                                Text(
                                    text = gapCaption(
                                        normalized = normalized,
                                        days = stats.daysInRange,
                                        dayType = filter.dayType,
                                        hasUnplanned = stats.aspirationGap.any { it.plannedHours == 0 }
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = TextColour
                                )
                            }
                        }
                    }

                    // -------- TRANSITIONS --------
                    item {
                        StatsCard(title = "What Happens After") {
                            val matrix =
                                if (changesOnly) stats.transitionChangesMatrix else stats.transitionMatrix
                            if (matrix.axis.none { it != null }) {
                                Text(
                                    if (changesOnly) {
                                        "No changes of activity in this range"
                                    } else {
                                        "Nothing logged in this range"
                                    }, color = TextColour
                                )
                            } else {
                                StatsChipRow(
                                    options = listOf(true, false),
                                    selected = normalized,
                                    label = {
                                        when (it) {
                                            true -> "% next"
                                            false -> "Hours"
                                        }
                                    },
                                    onSelect = viewModel::setStatsNormalize
                                )
                                StatsChipRow(
                                    options = listOf(false, true),
                                    selected = changesOnly,
                                    label = {
                                        when (it) {
                                            false -> "All hours"
                                            true -> "Changes only"
                                        }
                                    },
                                    onSelect = viewModel::setStatsChangesOnly
                                )
                                Text(
                                    text = "Rows are this hour, columns are the hour after",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = TextColour
                                )
                                ActivityMatrixGrid(
                                    matrix = matrix,
                                    normalised = normalized
                                )
                                SwatchLegend(transitionLegend(matrix.axis))
                                Text(
                                    text = transitionCaption(stats.daysInRange, filter.dayType),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = TextColour
                                )
                            }
                        }
                    } // END Transitions
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
    return "Each column is one hour of the day, ${acrossDays(days, dayType)}"
}

// The caption under the aspiration gap. It names the *days* (dayList), like rhythmCaption,
// because this chart filters on days, not nights. In % mode it also warns that bar lengths
// can't be compared between rows: each row is scaled to its own longer bar.
private fun gapCaption(normalized: Boolean, days: Int, dayType: DayType, hasUnplanned: Boolean): String {
    val range = acrossDays(days, dayType)

    if (!normalized) return "Actual / planned hours, $range"

    val unplannedNote = if (hasUnplanned) " N/A means logged but never planned." else ""
    return "Actual as a share of planned, $range. " +
            "Each row is scaled to its own longer bar, so compare rows by their numbers, not their lengths." +
            unplannedNote
}

// The caption under portion of time. "Since your first log" is the part that matters: this
// chart ignores the window chips, and without saying so it looks broken when they don't move it.
private fun shareCaption(dayType: DayType): String =
    "Each week's share of its hours, since your first log${dayNamesSuffix(dayType, dayType.dayList)}"

private fun shareReadout(label: String, share: Float?, weekStart: LocalDate): String {
    val month = weekStart.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
    val year = if (weekStart.year != LocalDate.now().year) " ${weekStart.year}" else ""
    val week = "the week of $month ${weekStart.dayOfMonth}$year"
    val percent = when {
        share == null -> return "$label · no hours in $week"
        share > 0f && share < 0.005f -> "<1%"
        else -> "${(share * 100).roundToInt()}%"
    }
    return "$label · $percent of $week"
}

private fun sleepReadout(day: DayOfWeek, night: SleepAverage?, is24Hour: Boolean): String {
    val name = day.getDisplayName(DateTextStyle.FULL, Locale.getDefault())
    if (night == null) return "No $name nights in this range"
    val bed = formatClockLabel(night.bedMinuteOfDay, is24Hour)
    val wake = formatClockLabel(night.wakeMinuteOfDay, is24Hour)
    val nights = if (night.nights == 1) "1 night" else "${night.nights} nights"
    return "$name night · bed $bed · wake $wake · ${formatDurationLabel(night.hours)} asleep, averaged over $nights"
}

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

private fun gapBars(gaps: List<ActivityGap>, normalized: Boolean): List<PairedBar>{
    return gaps.map { gap ->
        val maxOne = max(gap.plannedHours, gap.actualHours)
        val denominator = if(normalized){maxOne} else {1}
        val percentOfPlanned = if (gap.plannedHours != 0) {"${(gap.actualHours*100f/gap.plannedHours).roundToInt()}%"} else {"N/A"}
        PairedBar(
            label = gap.activity.name,
            colour = Color(gap.activity.colour),
            ghost = gap.plannedHours.toFloat()/denominator.toFloat(),
            solid = gap.actualHours.toFloat()/denominator.toFloat(),
            valueText = if (normalized) {percentOfPlanned} else {"${gap.actualHours} / ${gap.plannedHours}h"}
        )
    }
}

// One legend entry per axis slot, in axis order, so the legend reads in the same order as the
// grid's swatches. A null slot is the Unlogged row and column.
private fun transitionLegend(axis: List<Activity?>): List<Pair<String, Color>> =
    axis.map { activity ->
        if (activity == null) UNLOGGED_LABEL to UnloggedColour
        else activity.name to Color(activity.colour)
    }

// "across 12 days · Sat, Sun": the range phrase every day-based caption ends with.
private fun acrossDays(days: Int, dayType: DayType): String {
    val dayWord = if (days == 1) "day" else "days"
    return "across $days $dayWord${dayNamesSuffix(dayType, dayType.dayList)}"
}

private fun transitionCaption(days: Int, dayType: DayType): String =
    "Each row shows where the hour after an activity went, ${acrossDays(days, dayType)}"
