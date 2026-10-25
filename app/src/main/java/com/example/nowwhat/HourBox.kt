package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.BoxBorderGrey
import com.example.nowwhat.ui.theme.BoxFillGrey
import com.example.nowwhat.ui.theme.inkOn

@Composable
fun HourBox(
    hourSlot: HourSlot?,
    onClick: () -> Unit, // callback
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    showInitial: Boolean = false
) {
    val actual = hourSlot?.actual
    val planned = hourSlot?.planned

    val borderColour = hourSlot?.planned?.colour
        ?.let { Color(it) }
        ?: BoxBorderGrey

    val fillColour = actual?.colour
        ?.let { Color(it) }
        ?: BoxFillGrey

    val shape = RoundedCornerShape(8.dp)
    val selectedShape = RoundedCornerShape(12.dp)
    val displayShape = if (isSelected) selectedShape else shape

    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(
                elevation = if (isSelected) 10.dp else 0.dp,
                shape = displayShape
            )
            .border(2.dp, borderColour, displayShape)
            .clip(displayShape)
            .background(fillColour)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (showInitial) {
            when {
                // Logged: the actual's letter, ink picked for legibility on its fill
                actual != null -> InitialLabel(actual.name, inkOn(fillColour))
                // Plan only: the planned letter in the planned colour, on the empty box
                planned != null -> InitialLabel(planned.name, Color(planned.colour))
            }
        }
    }
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
        HourBox(hourSlot = testHour1, isSelected = true, onClick = {})
        HourBox(hourSlot = testHour2, onClick = {})
        HourBox(hourSlot = testHour3, onClick = {})
    }
}