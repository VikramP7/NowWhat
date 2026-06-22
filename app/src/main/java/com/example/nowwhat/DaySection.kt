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

    val dayPartLabels = listOf<String>("Morning (6-12)", "Day (12-6)", "Evening (6-12)", "Night (12-6)")

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

}