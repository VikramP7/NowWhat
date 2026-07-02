package com.example.nowwhat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.CancelGrey
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun EntryPanel(
    activityList: List<Activity>,
    selectedTimestamp: Long,
    selectedIsFuture: Boolean,
    onPlannedClick: (activityIndex: Int) -> Unit, // callback
    onActualClick: (activityIndex: Int) -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(selectedTimestamp)
        .atZone(zone)

    val dayFormatter = DateTimeFormatter.ofPattern("MMM d")
    val hourFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val dayText = dateTime.format(dayFormatter)
    val startTime = dateTime.format(hourFormatter)
    val endTime = dateTime.plusHours(1).format(hourFormatter)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                top = 12.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp)
            .background(OffWhite)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.95f to Color.Transparent,
                            1f to OffWhite
                        )
                    )
                )
            }
    ) {
        Row() {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                tint = TextColour,
                contentDescription = "Clock"
            )
            Text(text = " ${startTime} - ${endTime} · ${dayText}",color = TextColour)
        }
        Text(text = "What's planned?", color = TextColour)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
            itemsIndexed(activityList, key = {_, activity -> activity.id}) {activityIndex, activity ->
                PresetButton(
                    activity= activity,
                    onClick={onPlannedClick(activityIndex)}
                )
            }
            item() {
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { onEditClick() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CancelGrey
                    ),
                    border = BorderStroke(2.dp, CancelGrey)
                ) {
                    Text("Edit+")
                }
            }
            item { Spacer(modifier = Modifier.height(22.dp)) }
        }

        Spacer(Modifier.height(8.dp))


        Text(text = "What's happened?", color = TextColour)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
            itemsIndexed(activityList, key = {_, activity -> activity.id}) {activityIndex, activity ->
                PresetButton(
                    activity= activity,
                    onClick={onActualClick(activityIndex)},
                    filled = true,
                    enabled = !selectedIsFuture
                )
            }
            item() {
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { onEditClick() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CancelGrey
                    ),
                    border = BorderStroke(2.dp, CancelGrey)
                ) {
                    Text("Edit+")
                }
            }
            item { Spacer(modifier = Modifier.height(22.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "Tap any hour above to edit it.", color = TextColour)

        Spacer(Modifier.height(8.dp))
        ActivityLegend(activityList=activityList)
    }
}

@Preview
@Composable
fun EntryPanelPreview() {
    val activities = listOf(
        Activity("Work", 0xFF003459.toInt(), id = 1),
        Activity("Sleep", 0xFFE55934.toInt(), id = 2),
        Activity("Gym", 0xFF00A7E1.toInt(), id = 3),
        Activity("Social", 0xFFE55934.toInt(), id = 4),
        Activity("Dating", 0xFFF48FB1.toInt(), id = 5)
    )
}