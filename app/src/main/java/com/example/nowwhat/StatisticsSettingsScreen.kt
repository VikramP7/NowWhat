package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.OrphanedColour
import com.example.nowwhat.ui.theme.UnloggedColour

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

                val slices: MutableList<DonutSlice> =
                    stats.actualActivityTotals.filter { it.hours>0 }.map {
                        DonutSlice(it.activity.name, Color(it.activity.colour), it.hours)
                }.toMutableList()
                if (stats.orphanedLogHours>0) slices.add(DonutSlice("Deleted Activities", OrphanedColour, stats.orphanedLogHours ))
                if (stats.blankLogHours>0) slices.add(DonutSlice("Unlogged", UnloggedColour, stats.blankLogHours ))

                item { DonutChart(
                    slices = slices,
                    ringThickness = 128.dp,
                    centreContent = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.loggedHours}",style = MaterialTheme.typography.displaySmall)
                            Text("of ${stats.hoursInRange} h",style = MaterialTheme.typography.titleMedium)
                        }
                    }
                ) }

                item {
                    DonutLegend(
                        slices = slices
                    )
                }
                
                item {
                    Text(
                        text = "Days tracked: ${stats.daysTracked}/${stats.daysInRange}\n" +
                                "Hours logged: ${stats.loggedHours}/${stats.hoursInRange}\n" +
                                "Hours planned: ${stats.plannedHours}/${stats.hoursInRange}\n" +
                                "Blank loggable hours: ${stats.blankLogHours}/${stats.hoursInRange}\n" +
                                "Blank planable hours: ${stats.blankPlanHours}/${stats.hoursInRange}\n" +
                                "Orphaned Log hours: ${stats.orphanedLogHours}/${stats.hoursInRange}\n" +
                                "Orphaned Plan hours: ${stats.orphanedPlanHours}/${stats.hoursInRange}"
                    )
                }
                items(stats.actualActivityTotals, key = { it.activity.id }) { total ->
                    Text(text = "${total.activity.name}: ${total.hours} h")
                }
            }
        }
    }
}
