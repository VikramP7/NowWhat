package com.example.nowwhat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NowWhatSettingsScreen(
    modifier: Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onMainNavigate: () -> Unit
) {

    val activities by viewModel.activities.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopBarSettings(onClick = {onMainNavigate()}) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ActivitiesSettings(
                modifier = modifier,
                activities = activities,
                onAddActivity = {activity -> viewModel.addActivity(activity)},
                onUpdateActivity = {name, colour, id -> viewModel.updateActivity(name, colour, id)},
                onRemoveActivity = {activity -> viewModel.removeActivity(activity)}
            )

            // eventually I want to put a HoursView here so you can create default planned activities for your work or sleep schedule
        }
    }
}
