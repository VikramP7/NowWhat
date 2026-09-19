package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun DonutLegend(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.hours }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(slice.colour)
                )
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextColour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${slice.hours} h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextColour
                )
                Text(
                    text = if (total > 0) "%.1f%%".format(100f * slice.hours / total) else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextColour,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp)
                )
            }
        }
    }
}