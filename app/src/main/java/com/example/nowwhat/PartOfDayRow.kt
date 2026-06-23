package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PartOfDayRow(
    hourSlots: List<HourSlot?>,
    label: String,
    onClick: (Int) -> Unit, // callback
    modifier: Modifier = Modifier,
    selectedHourInRow: Int? = null
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.4f)
                .align(Alignment.CenterVertically)
                .padding(end = 4.dp)
        )

        hourSlots.forEachIndexed { index, hourSlot ->
            HourBox(
                hourSlot = hourSlot,
                isSelected = (selectedHourInRow == index),
                onClick = { onClick(index) }
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
        PartOfDayRow(label = "Morning", hourSlots = listOf(testHour0, testHour1, testHour2, testHour3, testHour0, testHour2), selectedHourInRow = 2, onClick = {})
        PartOfDayRow(label = "Day", hourSlots = listOf(testHour1, testHour2, testHour3, testHour0, testHour2, testHour0), onClick = {})
    }
}