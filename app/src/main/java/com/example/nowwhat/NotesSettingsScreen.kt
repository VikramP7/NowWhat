package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
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
import com.example.nowwhat.ui.theme.BackgroundColour
import java.time.LocalDate

@Composable
fun NotesSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val notes by viewModel.notes.collectAsState()
    var noteDialog by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundColour,
        topBar = {
            TopBarSettings(
                onClick = { onNavigate(AppScreenState.SETTINGS) },
                path = " > Notes"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (notes.isEmpty()){
                item { Text(text = "No notes yet. Tap the note icon on any day.") }
            }

            items(notes, key = {it.epochDay}) { note ->
                NoteCard(
                    dateLabel = LocalDate.ofEpochDay(note.epochDay).format(DateFormatter),
                    text = note.text,
                    onClick = { noteDialog = note }
                )
            }
        }
        noteDialog?.let { note ->
            NoteDialog(
                title = LocalDate.ofEpochDay(note.epochDay).format(DateFormatter),
                initialText = note.text,
                onSave = { noteText -> viewModel.saveNote(note.epochDay, noteText)},
                onDismiss = { noteDialog = null }
            )
        }
    }
}
