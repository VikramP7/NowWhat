package com.example.nowwhat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.NowWhatTheme

enum class AppScreenState {MAIN, SETTINGS}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val screenState = remember { mutableStateOf(AppScreenState.MAIN)}
            val viewModel: NowWhatViewModel = viewModel()

            NowWhatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when(screenState.value){
                        AppScreenState.MAIN -> NowWhatScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            onSettingsNavigate = {screenState.value = AppScreenState.SETTINGS}
                        )

                        AppScreenState.SETTINGS -> NowWhatSettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            onMainNavigate = {screenState.value = AppScreenState.MAIN}
                        )
                    }

                }
            }
        }
    }
}