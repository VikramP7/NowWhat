package com.example.nowwhat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun DataSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportTo(it) { success ->
                Toast.makeText(
                    context,
                    if (success) "Backup exported" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFrom(it) { success ->
                Toast.makeText(
                    context,
                    if (success) "Backup restored" else "Import failed — data unchanged",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var showImportConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = { TopBarSettings(
            onClick = { onNavigate(AppScreenState.SETTINGS) },
            path = " > Import/Export Data"
        ) }
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRow(
                label = "Export Backup",
                onClick = { exportLauncher.launch("nowwhat-backup.json") }
            ) {
                Icon(painterResource(R.drawable.ic_fileexport), null, tint = TextColour)
            }

            SettingRow(
                label = "Import Backup",
                onClick = { showImportConfirm = true}
            ) {
                Icon(painterResource(R.drawable.ic_filesave), null, tint = TextColour)
            }
        }

        if (showImportConfirm) {
            ConfirmDialog(
                title = "Import backup?",
                message = "This replaces ALL current data: activities, hours, and schedule with the backup file. This can't be undone.",
                confirmLabel = "Choose file",
                onConfirm = { importLauncher.launch(arrayOf("application/json")) },
                onDismiss = { showImportConfirm = false }
            )
        }
    }
}