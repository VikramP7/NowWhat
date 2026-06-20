package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PartOfDayRow(
    hourSlots: List<HourSlot?>,
    label: String,
    hourOffset: Int,
    onClick: (Int) -> Unit, // callback
    modifier: Modifier = Modifier
) {
    Row(modifier=modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)){
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(0.5f)
                .align(Alignment.CenterVertically)
                .padding(end=8.dp)
        )

        hourSlots.forEachIndexed { index, hourSlot ->
            HourBox(
                hourSlot,
                onClick = {onClick(hourOffset+index)}
            )
        }
    }
}

@Preview
@Composable
fun PartOfDayRowPreview() {
    val gymActivity = Activity("Gym", 0xFF32a852.toInt())
    val sleepActivity = Activity("Sleeping", 0xFF1422c4.toInt())

    val testHour0 = HourSlot(gymActivity, sleepActivity)
    val testHour1 = HourSlot(planned = gymActivity)
    val testHour2 = HourSlot(actual = gymActivity)
    val testHour3 = HourSlot()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PartOfDayRow(label = "morning", hourOffset = 4, hourSlots = listOf(testHour0, testHour1, testHour2, testHour3, testHour0, testHour2), onClick = {})
        PartOfDayRow(label = "Day", hourOffset = 10, hourSlots = listOf(testHour1, testHour2, testHour3, testHour0, testHour2, testHour0), onClick = {})
        PartOfDayRow(label = "Evening", hourOffset = 16, hourSlots = listOf(testHour3, testHour0, testHour2, testHour2, testHour3, testHour0), onClick = {})
    }
}