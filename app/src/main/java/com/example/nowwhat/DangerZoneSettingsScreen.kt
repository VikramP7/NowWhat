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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.DangerRed

private enum class DangerKind { HOURS, NOTES, ACTIVITIES, SCHEDULE }

private data class DangerAction(
    val label: String,
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

@Composable
fun DangerZoneSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {
    // Saved: an enum fits in the bundle. The lambdas can't, so they're rebuilt
    // every time and looked up by this key.
    var pendingKind by rememberSaveable { mutableStateOf<DangerKind?>(null) }

    val actions = mapOf(
        DangerKind.HOURS to DangerAction(
            label = "Delete All Logged Hours",
            title = "Delete ALL logged hours?",
            message = "This PERMANENTLY deletes every logged hour. Your activities, notes, and schedule stay. This can't be undone!",
            onConfirm = { viewModel.clearAllHourEntries() }
        ),
        DangerKind.NOTES to DangerAction(
            label = "Delete All Logged Notes",
            title = "Delete ALL logged notes?",
            message = "This PERMANENTLY deletes every logged note. Your hours, activities and schedule stay. This can't be undone!",
            onConfirm = { viewModel.deleteAllNotes() }
        ),
        DangerKind.ACTIVITIES to DangerAction(
            label = "Reset Activities",
            title = "Reset Activities to default?",
            message = "This resets to default Activities, hours logged with removed activities will appear blank. This can't be undone!",
            onConfirm = { viewModel.resetActivities() }
        ),
        DangerKind.SCHEDULE to DangerAction(
            label = "Clear Default Schedule",
            title = "Clear the default schedule?",
            message = "This clears the default schedule for all days. This can't be undone!",
            onConfirm = { viewModel.clearAllScheduleSlots() }
        )
    )
    val pending = pendingKind?.let { actions[it] }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundColour,
        topBar = { TopBarSettings(
            onClick = { onNavigate(AppScreenState.SETTINGS) },
            path = " > DANGER ZONE"
        ) }
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actions.forEach { (kind, action) ->
                SettingRow(label = action.label, onClick = { pendingKind = kind }) {
                    Icon(painterResource(R.drawable.ic_warning), null, tint = DangerRed)
                }
            }
        }

        pending?.let { action ->
            ConfirmDialog(
                title = action.title,
                message = action.message,
                onConfirm = action.onConfirm,   // passed along, not called: ConfirmDialog calls it
                onDismiss = { pendingKind = null }
            )
        }
    }
}
