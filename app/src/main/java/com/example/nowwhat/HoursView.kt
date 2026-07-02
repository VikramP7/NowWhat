package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.OffWhite
import java.time.Instant
import java.time.ZoneId

@Composable
fun HoursView(
    days: List<Day>,
    onClick: (dayIndex: Int, hourIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedTimestamp: Long = 0L
) {

    val selectedZoned = Instant.ofEpochMilli(selectedTimestamp)
        .atZone(ZoneId.systemDefault())
    val selectedDate = logicalDateOf(selectedTimestamp)
    val selectedHour = selectedZoned.hour

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(OffWhite),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        itemsIndexed(days, key = { _, day -> day.date }) { dayIndex, day ->
            Column {
                if (dayIndex > 0) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                DaySection(
                    day = day,
                    selectedHourOfDay = if (day.localDate == selectedDate) selectedHour else null,
                    onClick = { hourIndex -> onClick(dayIndex, hourIndex) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Preview
@Composable
fun HoursViewPreview() {
}