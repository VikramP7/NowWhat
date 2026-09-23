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
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
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
                item { Text(text = "Crunching the numbers...") }
            } else {

                item {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp))
                    {
                        HeadlineStat(
                            value = "${stats.loggedHours}",
                            label = "Hours Logged",
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        HeadlineStat(
                            value = "${stats.daysTracked}/${stats.daysInRange}",
                            label = "Days Tracked",
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        HeadlineStat(
                            value = if (stats.coverage != null)
                                "${(stats.coverage*100).roundToInt()} %" else "N/A",
                            label = "Coverage",
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                val slices: MutableList<DonutSlice> =
                    stats.actualActivityTotals.filter { it.hours>0 }.map {
                        DonutSlice(it.activity.name, Color(it.activity.colour), it.hours)
                }.toMutableList()
                if (stats.orphanedLogHours>0) slices.add(DonutSlice("Deleted Activities", OrphanedColour, stats.orphanedLogHours ))
                if (stats.blankLogHours>0) slices.add(DonutSlice("Unlogged", UnloggedColour, stats.blankLogHours ))

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
                            Text("No data in selected range...")
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
                                barColour = Color(stats.sleepActivity.colour)
                            )
                        }

                        // TEMPORARY until chunk 6 draws the bars
                        Column {
                            stats.nightlySleepAverages.forEach { (day, average) ->
                                val dayName = day.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
                                Text(
                                    text = if (average == null) "$dayName night: —"
                                    else "$dayName night: ${formatDurationLabel(average.hours)}, " +
                                            "${formatClockLabel(average.bedMinuteOfDay, is24Hour)}–" +
                                            "${formatClockLabel(average.wakeMinuteOfDay, is24Hour)} " +
                                            "(${average.nights})",
                                    color = TextColour
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeadlineStat(
                            value = sleepAverage?.let { formatClockLabel(it.bedMinuteOfDay, is24Hour) } ?: "—",
                            label = "Avg Bedtime",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            valueStyle = MaterialTheme.typography.titleLarge
                        )
                        HeadlineStat(
                            value = sleepAverage?.let { formatDurationLabel(it.hours) } ?: "—",
                            label = "Avg Sleep",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            valueStyle = MaterialTheme.typography.titleLarge
                        )
                        HeadlineStat(
                            value = sleepAverage?.let { formatClockLabel(it.wakeMinuteOfDay, is24Hour) } ?: "—",
                            label = "Avg Waketime",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
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
            }
        }
    }
}

// The caption under the three sleep cards. The night names come from DayType.nightList,
// the same list the average was filtered by, so the caption can't disagree with the numbers.
private fun sleepCaption(nights: Int?, dayType: DayType): String {
    if (nights == null) return "No complete nights in this range"

    val nightWord = if (nights == 1) "night" else "nights"
    val whichNights =
        if (dayType == DayType.ALL) ""
        else " · " + dayType.nightList.joinToString(", ") {
            it.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
        }
    return "Averaged over $nights $nightWord$whichNights"
}
