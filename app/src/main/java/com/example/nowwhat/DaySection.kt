package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun DaySection(
    hourRows: List<List<HourSlot?>>,
    dateLabel: String,
    is24Hour: Boolean,
    dayStartHour: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    noteButton: (@Composable () -> Unit)? = null,
    selectedHourOfDay: Int? = null,
) {
    // 0..23 hours past the day's start, or null
    val selectedOffset = selectedHourOfDay?.let {logicalHourOf (it, dayStartHour)}

    val bandNames = listOf("Morning", "Day", "Evening", "Night")
    val dayPartLabels = bandNames.mapIndexed { index, name ->
        "$name ${formatHourLabel(clockHourOf(index * BAND_HOURS, dayStartHour), is24Hour)}"
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = TextColour,
                modifier = Modifier.weight(1f)
            )
            noteButton?.invoke()
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            hourRows.forEachIndexed { index, row ->
                val selectedInRow =
                    if (selectedOffset != null && selectedOffset / BAND_HOURS == index) selectedOffset % BAND_HOURS
                    else null

                PartOfDayRow(
                    label = dayPartLabels[index],
                    hourSlots = row,
                    selectedHourInRow = selectedInRow,
                    onClick = { hourInRow -> onClick(clockHourOf(index * BAND_HOURS + hourInRow, dayStartHour)) }
                )
            }
        }
    }
}

@Preview
@Composable
fun DaySectionPreview() {
}