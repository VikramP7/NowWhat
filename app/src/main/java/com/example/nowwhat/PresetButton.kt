package com.example.nowwhat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun PresetButton(
    activity: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    if (filled) {
        Button(
            modifier = modifier,
            onClick = { onClick() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(activity.colour),
                contentColor = Color.White
            )
        ) {
            Text(activity.name)
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            onClick = { onClick() },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(activity.colour)
            ),
            border = BorderStroke(2.dp, Color(activity.colour))
        ) {
            Text(activity.name)
        }
    }
}

@Preview
@Composable
fun PresetButtonPreview() {
    val gymActivity = Activity("Gym", 0xFF32a852.toInt())
    val sleepActivity = Activity("Sleeping", 0xFF1422c4.toInt())

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PresetButton(activity = gymActivity, onClick= {})
        PresetButton(activity = sleepActivity, onClick= {}, filled = true)
    }
}