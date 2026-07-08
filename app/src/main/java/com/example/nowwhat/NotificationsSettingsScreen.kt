package com.example.nowwhat

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.NotificationHelper.postHourlyNotification
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm
import com.example.nowwhat.ui.theme.OffWhite

@Composable
@SuppressLint("MissingPermission")
fun NotificationsSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val activities by viewModel.activities.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // This callback fires LATER, after the user responds to the dialog
        if (isGranted) {
            postHourlyNotification(context)
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

        Column (Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp)){
            Text(text = "Coming Soon...\n Master notifications switch, do not disturb times, notification format, etc", Modifier.fillMaxWidth())
            Spacer(Modifier.height(40.dp))
            Button(
                modifier = Modifier,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        postHourlyNotification(context)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Magenta,
                    contentColor = Color.White
                ),
                enabled = true
            ) {
                Text("TEST NOTIFICATION")
            }

            Button(
                modifier = Modifier,
                onClick = {
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                                PackageManager.PERMISSION_GRANTED
                    } else true

                    if (hasPermission) {
                        scheduleNextAlarm(context)
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                enabled = true
            ) {
                Text("Schedule NOTIFICATION")
            }
        }
    }
}
