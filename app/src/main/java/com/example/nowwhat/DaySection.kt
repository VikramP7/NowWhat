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
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun DaySection(
    day: Day,
    is24Hour: Boolean,
    dayStartHour: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedHourOfDay: Int? = null
) {
    // 0..23 hours past the day's start, or null
    val selectedOffset = selectedHourOfDay?.let { (it - dayStartHour + 24) % 24 }

    val bandNames = listOf("Morning", "Day", "Evening", "Night")
    val dayPartLabels = bandNames.mapIndexed { index, name ->
        "$name ${formatHourLabel((dayStartHour + index * 6) % 24, is24Hour)}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = day.date,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(end = 8.dp),
            color = TextColour
        )

        day.hourRows.forEachIndexed { index, row ->
            val selectedInRow =
                if (selectedOffset != null && selectedOffset / 6 == index) selectedOffset % 6
                else null

            PartOfDayRow(
                label = dayPartLabels[index],
                hourSlots = row,
                selectedHourInRow = selectedInRow,
                onClick = { hourInRow -> onClick((dayStartHour + (index * 6) + hourInRow) % 24) }
            )
        }
    }
}

@Preview
@Composable
fun DaySectionPreview() {
}