package com.example.nowwhat

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first

@Composable
fun LaunchPermissionPrompts() {
    val context = LocalContext.current

    // Both survive rotation, so turning the phone doesn't re-ask or lose the dialog.
    var alreadyChecked by rememberSaveable { mutableStateOf(false) }
    var showAlarmExplainer by rememberSaveable { mutableStateOf(false) }

    fun needsAlarmAccess() = !NotificationHelper.canScheduleExactAlarms(context)

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Runs later, after the user answers the system dialog.
        // Only ask for alarms if they said yes to notifications.
        if (granted && needsAlarmAccess()) showAlarmExplainer = true
    }

    LaunchedEffect(Unit) {
        if (alreadyChecked) return@LaunchedEffect
        alreadyChecked = true

        // Read DataStore directly: the ViewModel's StateFlow would hand back its placeholder.
        val enabled = SettingsStore(context).notificationsEnabled.first()
        if (!enabled) return@LaunchedEffect

        if (!NotificationHelper.hasNotificationPermission(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (needsAlarmAccess()) {
            showAlarmExplainer = true
        }
    }

    if (showAlarmExplainer) {
        AlertDialog(
            onDismissRequest = { showAlarmExplainer = false },
            title = { Text("Allow hourly reminders") },
            text = {
                Text(
                    "NowWhat needs the \"Alarms & reminders\" permission to notify you " +
                            "at the top of every hour. Without it, no hourly notifications are sent."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAlarmExplainer = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(NotificationHelper.exactAlarmSettingsIntent(context))
                    }
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmExplainer = false }) { Text("Not now") }
            }
        )
    }
}