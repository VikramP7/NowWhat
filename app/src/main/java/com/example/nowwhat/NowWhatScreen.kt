package com.example.nowwhat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.OffWhite

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
        containerColor = OffWhite,
        topBar = { TopBar(onClick = {onSettingsNavigate()}) },
        bottomBar = {
            EntryPanel(
                modifier = Modifier,
                activityList = activities,
                selectedTimestamp = selectedTimestamp,
                onPlannedClick = { activityIndex ->
                    val activity = activities[activityIndex]
                    viewModel.logPlannedActivity(activity.id)
                },
                onActualClick = { activityIndex ->
                    val activity = activities[activityIndex]
                    viewModel.logActualActivity(activity.id)
                },
                onEditClick = { onSettingsNavigate() },
            )
        }
    ) { innerPadding ->
        val bottomBarPadding = innerPadding.calculateBottomPadding()
        HoursView(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                top = innerPadding.calculateTopPadding(),
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding())
                .drawWithContent {
                    drawContent()
                    // Top fade
                    val fadeHeightTop = 10.dp.toPx()
                    val fadeHeightBottom = 32.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(OffWhite, OffWhite.copy(alpha = 0f)),
                            startY = 0f,
                            endY = fadeHeightTop
                        )
                    )
                    // Bottom fade — positioned above the EntryPanel
                    val bottomBarPx = bottomBarPadding.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(OffWhite.copy(alpha = 0f), OffWhite),
                            startY = size.height - fadeHeightBottom,
                            endY = size.height
                        )
                    )
            },
            days = days,
            selectedTimestamp = selectedTimestamp,
            onClick = { dayIndex, hourIndex ->
                viewModel.selectHour(dayIndex, hourIndex)
            }
        )
    }
}