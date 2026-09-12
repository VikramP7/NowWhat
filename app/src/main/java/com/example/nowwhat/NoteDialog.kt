package com.example.nowwhat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.example.nowwhat.ui.theme.TextColour

/**
 * Edit one day's synopsis.
 *
 * Deliberately knows nothing about Note, Day, or the ViewModel. The caller
 * decides what "save" means (calling viewModel.saveNote with the right
 * epochDay), the same way ConfirmDialog leaves onConfirm to its caller.
 * That's what lets the day grid and the Notes screen share it.
 */
@Composable
fun NoteDialog(
    title: String,
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // TextFieldValue instead of a plain String so the cursor can start at the
    // END of existing text (a String field puts it at the start).
    // rememberSaveable keeps what you've typed if the phone rotates.
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_note), null, tint = TextColour) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                placeholder = { Text("Write a quick synopsis…") },
                minLines = 4,
                maxLines = 8,           // longer notes scroll inside the field
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextColour,
                    cursorColor = TextColour
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Open straight into typing. This lives inside the dialog's content
            // (not after AlertDialog) because the dialog is its own window: the
            // field only exists once this slot has composed.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(fieldValue.text); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = TextColour)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
