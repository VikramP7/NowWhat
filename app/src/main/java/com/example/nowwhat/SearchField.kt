package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.BoxFillGrey
import com.example.nowwhat.ui.theme.TextColour

/**
 * A one-line search box in the app's card style (same shell as SettingRow
 * and NoteCard: 12dp corners, 3dp shadow, BoxFillGrey).
 *
 * Knows nothing about notes. The caller owns the query string and decides
 * what it matches, the same way NoteDialog leaves "save" to its caller.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…"
) {
    val shape = RoundedCornerShape(12.dp)
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextColour),
        cursorBrush = SolidColor(TextColour),
        // The keyboard's enter key shows a magnifying glass, and pressing it
        // just closes the keyboard: the list already filters as you type.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        // Card chrome goes on the field itself, so the whole card is the
        // tap target that focuses it. Padding lives in decorationBox (below)
        // rather than here, for the same reason.
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = shape)
            .clip(shape)
            .background(BoxFillGrey),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,           // decorative; the placeholder says what it is
                    tint = TextColour.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )

                // Placeholder and the real text share one Box, so they sit in
                // exactly the same spot. Same trick as the activity-name field.
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextColour.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }

                // Clear button only while there's something to clear.
                // Plain Icon + clickable (not IconButton), like the delete
                // icon on Edit Activities, so it doesn't inflate to 48dp and
                // make this card taller than the notes beneath it.
                if (query.isNotEmpty()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Clear search",
                        tint = TextColour,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onQueryChange("") }
                    )
                }
            }
        }
    )
}

/**
 * [text] with every match of [query] drawn in [style].
 *
 * Which characters count as a match is decided by matchRanges alone, so the
 * highlight can never disagree with the filter that chose the note.
 * A blank query gives back the plain text.
 */
fun highlightMatches(text: String, query: String, style: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        append(text)
        matchRanges(text, query).forEach { range ->
            // IntRange is inclusive at both ends; addStyle's end is exclusive.
            addStyle(style, start = range.first, end = range.last + 1)
        }
    }
