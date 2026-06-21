package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NowWhatScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel()
) {

    val days by viewModel.days.collectAsState()
    val activities by viewModel.activities.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopBar(onClick = {}) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            HoursView(
                modifier = Modifier.weight(1f),
                days = days,
                onClick = { _, _ -> }
            )
            EntryPanel(
                activityList = activities,
                onPlannedClick = { activityIndex ->
                    val activity = activities[activityIndex]
                    val nextHour = System.currentTimeMillis().let { now ->
                        // Truncate to current hour, then add one hour
                        (now / 3_600_000 + 1) * 3_600_000
                    }
                    viewModel.logPlannedActivity(nextHour, activity.id)
                },
                onActualClick = { activityIndex ->
                    val activity = activities[activityIndex]
                    val lastHour = System.currentTimeMillis().let { now ->
                        // Truncate to current hour
                        (now / 3_600_000) * 3_600_000
                    }
                    viewModel.logActualActivity(lastHour, activity.id)
                },
                onEditClick = {}
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

    val days = listOf(
        Day(date = "Today · Thu Jun 19", hourRows = listOf(
            listOf(HourSlot(sleep, sleep), HourSlot(gym, gym), HourSlot(work, work),
                HourSlot(work, work), HourSlot(work, work), HourSlot(work, social)),
            listOf(HourSlot(work, work), HourSlot(work, work), HourSlot(work, work),
                HourSlot(work, gym), HourSlot(gym, gym), HourSlot(social, social)),
            listOf(HourSlot(social, social), HourSlot(social, social), HourSlot(planned = social),
                HourSlot(), HourSlot(), HourSlot()),
            listOf(HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep),
                HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep))
        )),
        Day(date = "Yesterday · Wed Jun 18", hourRows = listOf(
            listOf(HourSlot(sleep, sleep), HourSlot(sleep, gym), HourSlot(gym, gym),
                HourSlot(work, work), HourSlot(work, work), HourSlot(work, work)),
            listOf(HourSlot(work, work), HourSlot(work, social), HourSlot(social, social),
                HourSlot(work, work), HourSlot(work, work), HourSlot(gym, gym)),
            listOf(HourSlot(social, social), HourSlot(actual = social), HourSlot(planned = gym),
                HourSlot(social, social), HourSlot(sleep, sleep), HourSlot(sleep, sleep)),
            listOf(HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep),
                HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep))
        ))
    )
}


