package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DaySection(
    day: Day,
    onClick: (Int) -> Unit, // callback
    modifier: Modifier = Modifier
) {

    // Morn  Day  Even  Nite
    // 6     12   18    0    (hour-offset)
    // 0     1    2     3    (index)

    val dayPartLabels = listOf<String>("Morning", "Day", "Evening", "Night")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        Text(
            text = day.date,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(end=8.dp)
        )

        day.hourRows.forEachIndexed { index, row ->
            val hourOffset = ((index+1)%4)*6
            PartOfDayRow(
                label = dayPartLabels[index],
                hourSlots = row,
                onClick = {hourInRow -> onClick(hourOffset+hourInRow)}
            )
        }
    }
}

@Preview
@Composable
fun DaySectionPreview() {
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DaySection(day = day1, onClick = {})
        DaySection(day = day2, onClick = {})
    }
}