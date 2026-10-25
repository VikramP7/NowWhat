package com.example.nowwhat

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// First letter of the name, upper-cased. Works on code points rather than chars:
// an emoji is two chars (a surrogate pair), and name.first() would return half of it.
// A new activity starts with a blank name, so empty gives "".
fun initialOf(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ""
    return String(Character.toChars(trimmed.codePointAt(0))).uppercase()
}

@Composable
fun InitialLabel(name: String, ink: Color) {
    Text(
        text = initialOf(name),
        color = ink,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1
    )
}