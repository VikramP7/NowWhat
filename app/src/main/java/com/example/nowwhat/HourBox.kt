package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HourBox(
   hourSlot: HourSlot?,
   onClick: () -> Unit, // callback
   modifier: Modifier = Modifier
) {

    val borderColour = hourSlot?.planned?.colour
        ?.let {Color(it)}
        ?: Color.Transparent

    val fillColour = hourSlot?.actual?.colour
        ?.let {Color(it)}
        ?: Color.Transparent

    Box(
        modifier = modifier
            .size(50.dp)
            .border(4.dp, borderColour)
            .background(fillColour)
            .clickable(onClick=onClick)
    )
}

@Preview
@Composable
fun HourBoxPreview() {
    val gymActivity = Activity("Gym", 0xFF32a852.toInt())
    val sleepActivity = Activity("Sleeping", 0xFF1422c4.toInt())

    val testHour0 = HourSlot(gymActivity, sleepActivity)
    val testHour1 = HourSlot(planned = gymActivity)
    val testHour2 = HourSlot(actual = gymActivity)
    val testHour3 = HourSlot()

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HourBox(hourSlot = testHour0, onClick = {})
        HourBox(hourSlot = testHour1, onClick = {})
        HourBox(hourSlot = testHour2, onClick = {})
        HourBox(hourSlot = testHour3, onClick = {})
        HourBox(hourSlot = testHour2, onClick = {})
        HourBox(hourSlot = testHour0, onClick = {})
    }

}