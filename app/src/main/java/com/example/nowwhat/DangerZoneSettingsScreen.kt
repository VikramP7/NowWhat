package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.DangerRed
import com.example.nowwhat.ui.theme.BackgroundColour

private data class DangerAction(
    val title: String,
    val message: String,
    val icId: Int,
    val onConfirm: () -> Unit
)

@Composable
fun DangerZoneSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {
    var pendingAction by remember { mutableStateOf<DangerAction?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundColour,
        topBar = { TopBarSettings(
            onClick = {onNavigate(AppScreenState.SETTINGS)},
            path = " > DANGER ZONE"
        ) }
    ) { innerPadding ->
        Column (Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            SettingRow(
                label = "Delete All Logged Hours",
                onClick = {
                    pendingAction = DangerAction(
                        title = "Delete ALL logged hours?",
                        message = "This Permanently deletes every logged hour. Your activities and schedule stay. This can't be undone!",
                        icId = R.drawable.ic_warning,
                        onConfirm = {viewModel.clearAllHourEntries() }
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = DangerRed
                )
            }

            SettingRow(
                label = "Reset Activities",
                onClick = {
                    pendingAction = DangerAction(
                        title = "Reset Activities to default?",
                        message = "This resets to default Activities, hours logged with removed activities will appear blank. This can't be undone!",
                        icId = R.drawable.ic_warning,
                        onConfirm = {viewModel.resetActivities()}
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = DangerRed
                )
            }

            SettingRow(
                label = "Clear Default Schedule",
                onClick = {
                    pendingAction = DangerAction(
                        title = "Clear the default schedule?",
                        message = "This clears the default schedule for all days. This can't be undone!",
                        icId = R.drawable.ic_warning,
                        onConfirm = {viewModel.clearAllScheduleSlots()}
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = DangerRed
                )
            }
        }

        pendingAction?.let {
            ConfirmDialog(it.title, it.message, iconId = it.icId, onConfirm = it.onConfirm, onDismiss = { pendingAction = null })
        }
    }
}
