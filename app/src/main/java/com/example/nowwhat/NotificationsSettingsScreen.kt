package com.example.nowwhat

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour

@Composable
@SuppressLint("MissingPermission")
fun NotificationsSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val dndStartHour by viewModel.dndStartHour.collectAsState()
    val dndEndHour by viewModel.dndEndHour.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()
    val context = LocalContext.current

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    } else true

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // This callback fires LATER, after the user responds to the dialog
        if (isGranted) {
            scheduleNextAlarm(context)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = { TopBarSettings(
            onClick = {onNavigate(AppScreenState.SETTINGS)},
            path = " > Notifications"
        ) }
    ) { innerPadding ->

        Column (Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            SettingRow(label = "Notifications") {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        viewModel.setNotificationsEnabled(it)
                        if (!hasPermission && it) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OffWhite,
                        checkedTrackColor = TextColour,
                        uncheckedThumbColor = TextColour,
                        uncheckedTrackColor = OffWhite,
                        uncheckedBorderColor = TextColour
                    )
                )
            }

            SettingRow(label = "Do Not Disturb From", enabled = notificationsEnabled) {
                NumberStepper(
                    value = dndStartHour,
                    onValueChange = { viewModel.setDndStartHour(it) },
                    format = { formatHourLabel(it, is24Hour) },
                    enabled = notificationsEnabled
                )
            }

            SettingRow(label = "Do Not Disturb Until", enabled = notificationsEnabled) {
                NumberStepper(
                    value = dndEndHour,
                    onValueChange = { viewModel.setDndEndHour(it) },
                    format = { formatHourLabel(it, is24Hour)},
                    enabled = notificationsEnabled
                )
            }
        }
    }
}
