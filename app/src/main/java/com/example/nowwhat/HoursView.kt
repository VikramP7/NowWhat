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

    // Rows ordered: Morning, Day, Evening, Night
    val day1 = Day(date = "Thursday, June 19", hourRows = listOf(
        listOf(HourSlot(sleep, sleep), HourSlot(gym, gym), HourSlot(work, work),
            HourSlot(work, work), HourSlot(work, work), HourSlot(work, social)),
        listOf(HourSlot(work, work), HourSlot(work, work), HourSlot(work, work),
            HourSlot(work, gym), HourSlot(gym, gym), HourSlot(social, social)),
        listOf(HourSlot(social, social), HourSlot(social, social), HourSlot(planned = social),
            HourSlot(), HourSlot(), HourSlot()),
        listOf(HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep),
            HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep))
    ))

    val day2 = Day(date = "Friday, June 20", hourRows = listOf(
        listOf(HourSlot(sleep, sleep), HourSlot(sleep, gym), HourSlot(gym, gym),
            HourSlot(work, work), HourSlot(work, work), HourSlot(work, work)),
        listOf(HourSlot(work, work), HourSlot(work, social), HourSlot(social, social),
            HourSlot(work, work), HourSlot(work, work), HourSlot(gym, gym)),
        listOf(HourSlot(social, social), HourSlot(actual = social), HourSlot(planned = gym),
            HourSlot(), HourSlot(), HourSlot()),
        listOf(HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep),
            HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep))
    ))

    HoursView(days = listOf(day1, day2, day1, day2), onClick = { _, _ -> })
}