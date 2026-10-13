package com.example.nowwhat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.TextColour

/**
 * One row per slice: swatch, name, hours, percentage.
 *
 * @param selectedLabel the row to bold, by label (the same value DonutChart raises). Null bolds none.
 * @param onRowClick called with a row's label when it's tapped. Null makes the rows plain text,
 *   not tappable. As with the chart, the caller decides what a tap does.
 */
@Composable
fun DonutLegend(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    selectedLabel: String? = null,
    onRowClick: ((String) -> Unit)? = null
) {
    val total = slices.sumOf { it.hours }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        slices.forEach { slice ->
            // One weight for all three texts in the row, so the whole row reads as selected.
            val weight = if (slice.label == selectedLabel) FontWeight.Bold else null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Only tappable when the caller asked for it.
                    .then(if (onRowClick != null) Modifier.clickable { onRowClick(slice.label) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegendSwatch(colour = slice.colour)
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = weight,
                    color = TextColour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${slice.hours} h",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = weight,
                    color = TextColour
                )
                Text(
                    text = if (total > 0) "%.1f%%".format(100f * slice.hours / total) else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = weight,
                    color = TextColour,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp)
                )
            }
        }
    }
}
