package com.example.nowwhat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NowWhatScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onSettingsNavigate: () -> Unit
) {

    val days by viewModel.days.collectAsState()
    val activities by viewModel.activities.collectAsState()

    val selectedTimestamp by viewModel.selectedTimestamp.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopBar(onClick = {onSettingsNavigate()}) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            HoursView(
                modifier = Modifier.weight(1f),
                days = days,
                onClick = { dayIndex, hourIndex ->
                    viewModel.selectHour(dayIndex, hourIndex)
                }
            )
            EntryPanel(
                modifier = Modifier,
                activityList = activities,
                selectedTimestamp = selectedTimestamp,
                onPlannedClick = { activityIndex ->
                    val activity = activities[activityIndex]
                    viewModel.logPlannedActivity( activity.id)
                },
                onActualClick = { activityIndex ->
                    val activity = activities[activityIndex]
                    viewModel.logActualActivity( activity.id)
                },
                onEditClick = {onSettingsNavigate()}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NowWhatScreenPreview() {
    val activities = listOf(
        Activity("Work", 0xFF4CAF50.toInt(), id = 1),
        Activity("Sleep", 0xFF3F51B5.toInt(), id = 2),
        Activity("Gym", 0xFFFF9800.toInt(), id = 3),
        Activity("Social", 0xFFE91E63.toInt(), id = 4),
        Activity("Dating", 0xFFF48FB1.toInt(), id = 5)
    )

    val sleep = activities[1]
    val work = activities[0]
    val gym = activities[2]
    val social = activities[3]
}


