package com.example.nowwhat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.OffWhite

@Composable
fun NowWhatSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onMainNavigate: () -> Unit
) {

    val activities by viewModel.activities.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = { TopBarSettings(onClick = {onMainNavigate()}) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp)) {
            ActivitiesSettings(
                modifier = Modifier,
                activities = activities,
                onAddActivity = {activity -> viewModel.addActivity(activity)},
                onUpdateActivity = {name, colour, id -> viewModel.updateActivity(name, colour, id)},
                onRemoveActivity = {activity -> viewModel.removeActivity(activity)}
            )

            // eventually I want to put a HoursView here so you can create default planned activities for your work or sleep schedule
        }
    }
}
