package com.example.nowwhat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm
import com.example.nowwhat.ui.theme.NowWhatTheme

enum class AppScreenState {
    MAIN,
    SETTINGS,
    SETTINGS_ACTIVITIES,
    SETTINGS_DEFAULTSCHEDULE,
    SETTINGS_DANGERZONE,
    SETTINGS_DATA,
    SETTINGS_NOTIFICATIONS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        scheduleNextAlarm(this)
        setContent {

            val screenState = remember { mutableStateOf(AppScreenState.MAIN)}
            val viewModel: NowWhatViewModel = viewModel()

            NowWhatTheme {
                when(screenState.value){
                    AppScreenState.MAIN -> NowWhatScreen(
                        viewModel = viewModel,
                        onNavigate = {nextScreenState -> screenState.value = nextScreenState}
                    )

                    AppScreenState.SETTINGS -> NowWhatSettingsScreen(
                        viewModel = viewModel,
                        onNavigate = { nextScreenState -> screenState.value = nextScreenState}
                    )

                    AppScreenState.SETTINGS_ACTIVITIES -> ActivitiesSettingsScreen(
                        viewModel = viewModel,
                        onNavigate = {nextScreenState -> screenState.value = nextScreenState}
                    )

                    AppScreenState.SETTINGS_DEFAULTSCHEDULE -> DefaultScheduleSettingsScreen(
                        viewModel = viewModel,
                        onNavigate = {nextScreenState -> screenState.value = nextScreenState}
                    )

                    AppScreenState.SETTINGS_DANGERZONE -> DangerZoneSettingsScreen(
                        viewModel = viewModel,
                        onNavigate = {nextScreenState -> screenState.value = nextScreenState}
                    )

                    AppScreenState.SETTINGS_DATA -> DataSettingsScreen(
                        viewModel = viewModel,
                        onNavigate = {nextScreenState -> screenState.value = nextScreenState}
                    )

                    AppScreenState.SETTINGS_NOTIFICATIONS -> NotificationsSettingsScreen(
                        viewModel = viewModel,
                        onNavigate = {nextScreenState -> screenState.value = nextScreenState}
                    )
                }
            }
        }
    }
}