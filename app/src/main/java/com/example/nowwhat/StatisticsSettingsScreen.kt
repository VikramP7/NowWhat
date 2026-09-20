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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.OrphanedColour
import com.example.nowwhat.ui.theme.TextColour
import com.example.nowwhat.ui.theme.UnloggedColour
import kotlin.math.roundToInt

@Composable
fun StatisticsSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val statistics by viewModel.statistics.collectAsState()
    val filter by viewModel.statsFilter.collectAsState()

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
            }
        }
    }
}
