package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.BoxFillGrey
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun SettingRow(
    label: String? = null,                                  // now nullable — content can replace it
    textColour: Color = TextColour,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,              // NEW: optional left slot (the swatch)
    content: (@Composable RowScope.() -> Unit)? = null,     // NEW: optional middle override (the TextField)
    trailing: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = shape)
            .clip(shape)
            .background(BoxFillGrey)
            .alpha(if (enabled) 1f else 0.4f)
            .then(
                if (onClick != null && enabled) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        if (content != null) {
            content()                                       // caller supplies its own weight(1f)
        } else {
            Text(
                text = label ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = textColour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        trailing()
    }
}