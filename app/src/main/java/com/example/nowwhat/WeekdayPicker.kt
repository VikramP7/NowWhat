package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun WeekdayPicker(
    selectedDay: Int,               // 1–7, Mon–Sun (ISO)
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // index 0..6 -> ISO weekday 1..7
    val labels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            val isSelected = day == selectedDay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.background(TextColour)
                        else Modifier.border(1.dp, TextColour, CircleShape)
                    )
                    .clickable { onDaySelected(day) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) BackgroundColour else TextColour
                )
            }
        }
    }
}