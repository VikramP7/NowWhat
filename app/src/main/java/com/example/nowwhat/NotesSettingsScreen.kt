package com.example.nowwhat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.TextColour
import java.time.LocalDate

@Composable
fun NotesSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val notes by viewModel.notes.collectAsState()
    var noteDialogDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val noteDialog = notes.find { it.epochDay == noteDialogDay }
    val dayStartHour by viewModel.dayStartHour.collectAsState()

    // The search text. Presentation only, so it lives here rather than in
    // the ViewModel: survives rotation, resets when you leave the screen.
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()

    val highlight = SpanStyle(
        background = TextColour.copy(alpha = 0.15f),
        fontWeight = FontWeight.SemiBold
    )

    // Filter notes for the note text containing the query or the date label containing the query
    val shownNotes = if (q.isNotBlank()) {
        notes.filter { note ->
            matchRanges(note.text, q).isNotEmpty() ||
                    matchRanges(dateLabelOf(note, dayStartHour), q).isNotEmpty()
        }
    }else {
        notes
    }

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
        // pinned part on top, then the
        // list taking the rest with weight(1f). innerPadding is applied once,
        // here, and imePadding moved up with it so the list shrinks above the
        // keyboard while the search field stays put.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // No point offering search over nothing.
            if (notes.isNotEmpty()) {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search notes",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                // top = 8 leaves room for the first card's shadow, since a
                // LazyColumn clips at its edge (same as statistics).
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (notes.isEmpty()) {
                    item { Text(text = "No notes yet. Tap the note icon on any day.") }
                }
                else if (shownNotes.isEmpty()){
                    //a second empty state, for "there are notes, but none match the search"
                    item { Text(text = "No notes match \"$q\". Search looks in each note's text and date.") }
                }

                items(shownNotes, key = { it.epochDay }) { note ->
                    NoteCard(
                        dateLabel = highlightMatches(dateLabelOf(note, dayStartHour), q, highlight),
                        text = highlightMatches(searchPreview(note.text, q), q, highlight),
                        onClick = { noteDialogDay = note.epochDay }
                    )
                }
            }
        }
        noteDialog?.let { note ->
            NoteDialog(
                title = dateLabelOf(note, dayStartHour),
                initialText = note.text,
                onSave = { noteText -> viewModel.saveNote(note.epochDay, noteText) },
                onDismiss = { noteDialogDay = null }
            )
        }
    }
}

private fun dateLabelOf(note: Note, dayStartHour: Int): String {
    return formatDateLabel(
        LocalDate.ofEpochDay(note.epochDay),
        logicalDateOf(System.currentTimeMillis(), dayStartHour)
    )
}
