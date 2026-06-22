package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HoursView(
    days: List<Day>,
    onClick: (dayIndex: Int, hourIndex: Int) -> Unit, // callback
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(days, key = {_, day -> day.date}) {dayIndex, day ->
            DaySection(
                day = day,
                onClick= {hourIndex -> onClick(dayIndex, hourIndex)}
            )
        }
    }
}

@Preview
@Composable
fun HoursViewPreview() {
    val gym = Activity("Gym", 0xFF4CAF50.toInt())
    val sleep = Activity("Sleep", 0xFF3F51B5.toInt())
    val work = Activity("Work", 0xFFFF9800.toInt())
    val social = Activity("Social", 0xFFE91E63.toInt())
}