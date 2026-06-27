package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.TextColour


@Composable
fun ActivityLegend(
    activityList: List<Activity>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(activityList) { activity ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(activity.colour))
                )
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextColour
                )
            }
        }
    }
}

@Preview
@Composable
fun ActivityLegendPreview() {
    val gymActivity = Activity("Gym", LightActivityColours[0])
    val sleepActivity = Activity("Sleeping", LightActivityColours[1])

    ActivityLegend(activityList = listOf(gymActivity, sleepActivity))
}