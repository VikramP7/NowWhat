package com.example.nowwhat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.CancelGrey
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun DefaultScheduleSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val activities by viewModel.activities.collectAsState()
    val days by viewModel.days.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = { TopBarSettings(
            onClick = {onNavigate(AppScreenState.SETTINGS)},
            path = " > Default Schedule"
        ) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp)){
            val today = days.firstOrNull()
            if (today != null) {
                DaySection(day = today, onClick = {/*handle selection and have back end to keep track of what hour is selected in default schedule*/})
            }

            Text(text = "Default schedule planned?", color = TextColour)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                itemsIndexed(activities, key = {_, activity -> activity.id}) {activityIndex, activity ->
                    PresetButton(
                        activity= activity,
                        onClick={/*viewModel.logDefaultScheduledHour*/}
                    )
                }
                item() {
                    OutlinedButton(
                        modifier = Modifier,
                        onClick = { onNavigate(AppScreenState.SETTINGS_ACTIVITIES) },
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

        }

    }
}
