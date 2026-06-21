package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun EntryPanel(
    activityList: List<Activity>,
    onPlannedClick: (activityIndex: Int) -> Unit, // callback
    onActualClick: (activityIndex: Int) -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editActivity = Activity(name = "Edit+", colour = 0xffcccccc.toInt())

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row() {
            Text("clockIcon")
            Text(text = "Last Hour · 5:00-6:00")
        }
        Text(text = "What Will you do?")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
            itemsIndexed(activityList, key = {_, activity -> activity.id}) {activityIndex, activity ->
                PresetButton(
                    activity= activity,
                    onClick={onPlannedClick(activityIndex)}
                )
            }
            item() {
                PresetButton(
                    activity = editActivity,
                    onClick = { onEditClick() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row() {
            Text("clockIcon")
            Text(text = "Last Hour · 4:00-5:00")
        }
        Text(text = "What Will you do?")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
            itemsIndexed(activityList, key = {_, activity -> activity.id}) {activityIndex, activity ->
                PresetButton(
                    activity= activity,
                    onClick={onActualClick(activityIndex)}
                )
            }
            item() {
                PresetButton(
                    activity = editActivity,
                    onClick = { onEditClick() }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "Tap any hour above to edit it.")

        Spacer(Modifier.height(8.dp))
        ActivityLegend(activityList= activityList)
    }
}

@Preview
@Composable
fun EntryPanelPreview() {
    val activities = listOf(
        Activity("Work", 0xFF4CAF50.toInt(), id = 1),
        Activity("Sleep", 0xFF3F51B5.toInt(), id = 2),
        Activity("Gym", 0xFFFF9800.toInt(), id = 3),
        Activity("Social", 0xFFE91E63.toInt(), id = 4),
        Activity("Dating", 0xFFF48FB1.toInt(), id = 5)
    )

    EntryPanel(
        activityList = activities,
        onPlannedClick = {},
        onActualClick = {},
        onEditClick = {}
    )
}