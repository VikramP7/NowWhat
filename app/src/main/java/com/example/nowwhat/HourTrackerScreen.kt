package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HourTrackerScreen(
    modifier: Modifier = Modifier,
    viewModel: HourViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())

    var didWhat by remember { mutableStateOf("") }
    var nowWhat by remember { mutableStateOf("") }

    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("What did you just do?", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = didWhat,
            onValueChange = { didWhat = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text("What will you do next?", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = nowWhat,
            onValueChange = { nowWhat = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (didWhat.isNotBlank() || nowWhat.isNotBlank()) {
                    viewModel.addEntry(didWhat, nowWhat)
                    didWhat = ""
                    nowWhat = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log this hour")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formatter.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (entry.didWhat.isNotBlank()) Text("Did: ${entry.didWhat}")
                    if (entry.nowWhat.isNotBlank()) Text("Next: ${entry.nowWhat}")
                }
            }
        }
    }
}