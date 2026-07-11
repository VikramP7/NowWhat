package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.DangerRed
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun NowWhatSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val is24Hour by viewModel.is24Hour.collectAsState()
    val dayStartHour by viewModel.dayStartHour.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = { TopBarSettings(onClick = {onNavigate(AppScreenState.MAIN)}) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRow(
                label = "Edit Activities",
                onClick = {onNavigate(AppScreenState.SETTINGS_ACTIVITIES)}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = TextColour
                )
            }

            SettingRow(
                label = "Edit Default Schedule",
                onClick = {onNavigate(AppScreenState.SETTINGS_DEFAULTSCHEDULE)}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = TextColour
                )
            }

            SettingRow(label = "Day Starts At") {
                NumberStepper(
                    value = dayStartHour,
                    onValueChange = { viewModel.setDayStartHour(it) },
                    format = { formatHourLabel(it, is24Hour) }
                )
            }

            SettingRow(label = "24-Hour Time") {
                Switch(
                    checked = is24Hour,
                    onCheckedChange = { viewModel.setIs24Hour(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OffWhite,
                        checkedTrackColor = TextColour,
                        uncheckedThumbColor = TextColour,
                        uncheckedTrackColor = OffWhite,
                        uncheckedBorderColor = TextColour
                    )
                )
            }

            SettingRow(
                label = "Notifications",
                onClick = {onNavigate(AppScreenState.SETTINGS_NOTIFICATIONS)}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = TextColour
                )
            }

            SettingRow(
                label = "Export/Import Data",
                onClick = {onNavigate(AppScreenState.SETTINGS_DATA)}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = TextColour
                )
            }

            SettingRow(
                label = "DANGER ZONE",
                textColour = DangerRed,
                onClick = {onNavigate(AppScreenState.SETTINGS_DANGERZONE)}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = null,
                    tint = TextColour
                )
            }
        }
    }
}
