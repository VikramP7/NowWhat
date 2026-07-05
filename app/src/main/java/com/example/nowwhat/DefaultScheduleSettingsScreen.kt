package com.example.nowwhat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.CancelGrey
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DefaultScheduleSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val activities by viewModel.activities.collectAsState()
    val scheduleRows by viewModel.scheduleRows.collectAsState()
    val selectedDay by viewModel.selectedScheduleDay.collectAsState()
    val selectedHour by viewModel.selectedScheduleHour.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()
    val dayStartHour by viewModel.dayStartHour.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = { TopBarSettings(
            onClick = {onNavigate(AppScreenState.SETTINGS)},
            path = " > Default Schedule"
        ) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeekdayPicker(
                selectedDay = selectedDay,
                onDaySelected = { viewModel.setSelectedScheduleDay(it) }
            )

            DaySection(
                hourRows = scheduleRows,
                dateLabel = DayOfWeek.of(selectedDay).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                is24Hour = is24Hour,
                dayStartHour = dayStartHour,
                selectedHourOfDay = selectedHour,
                onClick = { hour -> viewModel.setSelectedScheduleHour(hour) }
            )

            Row() {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    tint = TextColour,
                    contentDescription = "Clock"
                )

                val slotWeekday = scheduleWeekdayFor(selectedHour, selectedDay, dayStartHour)
                val slotDayName = DayOfWeek.of(slotWeekday).getDisplayName(TextStyle.SHORT, Locale.getDefault())

                Text(text = " ${selectedHour}:00 - ${selectedHour+1}:00 · ${slotDayName} · What's planned?",color = TextColour)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.logClearSelectedActivity() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CancelGrey),
                        border = BorderStroke(2.dp, CancelGrey)
                    ) { Text("Clear") }
                }
                itemsIndexed(activities, key = { _, activity -> activity.id }) { _, activity ->
                    PresetButton(
                        activity = activity,
                        onClick = { viewModel.logScheduledActivity(activity.id) }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { onNavigate(AppScreenState.SETTINGS_ACTIVITIES) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CancelGrey),
                        border = BorderStroke(2.dp, CancelGrey)
                    ) { Text("Edit+") }
                }
            }
        }

    }
}
