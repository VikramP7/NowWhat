package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DaySection(
    day: Day,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedHourOfDay: Int? = null
) {

    // Morn  Day  Even  Nite
    // 6     12   18    0    (hour-offset)
    // 0     1    2     3    (index)

    val dayPartLabels = listOf("Morning 6am", "Day 12pm", "Evening 6pm", "Night 12am")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = day.date,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(end = 8.dp)
        )

        day.hourRows.forEachIndexed { index, row ->
            val hourOffset = ((index + 1) % 4) * 6

            val selectedInRow = if (
                selectedHourOfDay != null
                && selectedHourOfDay >= hourOffset
                && selectedHourOfDay < hourOffset + 6
            ) {
                selectedHourOfDay - hourOffset
            } else null

            PartOfDayRow(
                label = dayPartLabels[index],
                hourSlots = row,
                selectedHourInRow = selectedInRow,
                onClick = { hourInRow -> onClick(hourOffset + hourInRow) }
            )
        }
    }
}

@Preview
@Composable
fun DaySectionPreview() {
}