package com.example.nowwhat

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.DangerRed
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
    val noteNotificationsEnabled by viewModel.noteNotificationsEnabled.collectAsState()
    val noteNotificationHour by viewModel.noteNotificationHour.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var canScheduleAlarms by remember { mutableStateOf(NotificationHelper.canScheduleExactAlarms(context)) }
    // Can NowWhat actually post? Differs from hasPermission only below Android 13,
    // where there's no permission but the user can still switch notifications off.
    var notificationsAllowed by remember { mutableStateOf(NotificationHelper.notificationsAllowed(context)) }

    // Re-check whenever this screen comes back to the front, e.g. returning from system settings.
    LifecycleResumeEffect(Unit) {
        hasPermission = NotificationHelper.hasNotificationPermission(context)
        canScheduleAlarms = NotificationHelper.canScheduleExactAlarms(context)
        notificationsAllowed = NotificationHelper.notificationsAllowed(context)
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        notificationsAllowed = NotificationHelper.notificationsAllowed(context)
        if (isGranted) scheduleNextAlarm(context)
    }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundColour,
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
                        checkedThumbColor = BackgroundColour,
                        checkedTrackColor = TextColour,
                        uncheckedThumbColor = TextColour,
                        uncheckedTrackColor = BackgroundColour,
                        uncheckedBorderColor = TextColour
                    )
                )
            }

            SettingRow(
                label = "Alarms & Reminders",
                onClick = if (!canScheduleAlarms) {
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(NotificationHelper.exactAlarmSettingsIntent(context))
                        }
                    }
                } else null
            ) {
                Text(
                    text = if (canScheduleAlarms) "Allowed" else "Tap to allow",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (canScheduleAlarms) TextColour else DangerRed
                )
            }

            // Whether Android lets NowWhat post notifications at all. The switch
            // above can only ask through the system dialog, and after two
            // refusals Android stops showing it, so this row is the way back:
            // it opens the app's notification page in system settings, which
            // always works. LifecycleResumeEffect flips it to "Allowed" on return.
            // Dimmed while notifications are off, when the answer doesn't matter.
            SettingRow(
                label = "Notification Permission",
                enabled = notificationsEnabled,
                onClick = if (!notificationsAllowed) {
                    { context.startActivity(NotificationHelper.appNotificationSettingsIntent(context)) }
                } else null
            ) {
                Text(
                    text = if (notificationsAllowed) "Allowed" else "Tap to allow",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (notificationsAllowed) TextColour else DangerRed
                )
            }

            SettingRow(label = "Do Not Disturb From", enabled = notificationsEnabled) {
                NumberStepper(
                    value = dndStartHour,
                    onValueChange = { viewModel.setDndStartHour(it) },
                    format = { formatHourLabel(it, is24Hour) },
                    enabled = notificationsEnabled,
                    wrap = true
                )
            }

            SettingRow(label = "Do Not Disturb Until", enabled = notificationsEnabled) {
                NumberStepper(
                    value = dndEndHour,
                    onValueChange = { viewModel.setDndEndHour(it) },
                    format = { formatHourLabel(it, is24Hour)},
                    enabled = notificationsEnabled,
                    wrap = true
                )
            }

            SettingRow(label = "Note Reminder Notifications", enabled = notificationsEnabled) {
                Switch(
                    checked = noteNotificationsEnabled,
                    onCheckedChange = {
                        viewModel.setNoteNotificationsEnabled(it)
                        if (!hasPermission && it) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundColour,
                        checkedTrackColor = TextColour,
                        uncheckedThumbColor = TextColour,
                        uncheckedTrackColor = BackgroundColour,
                        uncheckedBorderColor = TextColour
                    ),
                    enabled = notificationsEnabled
                )
            }

            SettingRow(label = "Note Notification Hour", enabled = notificationsEnabled && noteNotificationsEnabled) {
                NumberStepper(
                    value = noteNotificationHour,
                    onValueChange = { viewModel.setNoteNotificationHour(it) },
                    format = { formatHourLabel(it, is24Hour)},
                    enabled = notificationsEnabled && noteNotificationsEnabled,
                    wrap = true
                )
            }
        }
    }
}
