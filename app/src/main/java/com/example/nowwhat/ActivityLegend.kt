package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.TextColour


// Turns activities into name/colour pairs and hands them to SwatchLegend.
@Composable
fun ActivityLegend(
    activityList: List<Activity>,
    modifier: Modifier = Modifier,
    wrap: Boolean = true
) {
    SwatchLegend(
        entries = activityList.map { it.name to Color(it.colour) },
        modifier = modifier,
        wrap = wrap
    )
}

// The one home for the swatch-and-name legend look. It takes plain name/colour pairs rather
// than activities, so it can also key things that aren't activities (Unlogged, Deleted
// Activities) without inventing fake Activity objects for them.
//
// wrap = true (the default) flows the entries onto as many lines as they need, so every entry is
// visible at once. wrap = false keeps them on one line that scrolls sideways, for a place where
// the legend's height must stay fixed (a pinned panel, say).
//
// selectedName bolds that entry. onEntryClick, when given, makes every entry tappable and reports
// its name; null keeps the legend plain text. As with the charts, the caller owns what a tap means.
@Composable
fun SwatchLegend(
    entries: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
    wrap: Boolean = true,
    selectedName: String? = null,
    onEntryClick: ((String) -> Unit)? = null
) {
    if (wrap) {
        // FlowRow fills a line left to right and starts a new line when the next entry won't fit.
        // horizontalArrangement spaces entries within a line; verticalArrangement spaces the lines.
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entries.forEach { (name, colour) -> LegendEntry(name, colour, name == selectedName, onEntryClick) }
        }
    } else {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { (name, colour) -> LegendEntry(name, colour, name == selectedName, onEntryClick) }
        }
    }
}

// One swatch and its name. Private, and shared by both layouts above, so wrapping or scrolling
// changes only how entries are arranged, never how an entry looks.
@Composable
private fun LegendEntry(
    name: String,
    colour: Color,
    selected: Boolean,
    onClick: ((String) -> Unit)?
) {
    Row(
        // Only tappable when the caller asked for it.
        modifier = if (onClick != null) Modifier.clickable { onClick(name) } else Modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendSwatch(colour = colour)
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else null,
            color = TextColour
        )
    }
}

// The one home for the swatch shape. Percent-based, so it's the same shape at any size:
// 25% of the legend's 12dp is 3dp, and the matrix headers (sized by their grid cell) match.
// SwatchLegend (and so ActivityLegend) and DonutLegend all draw their swatches with it.
val LegendSwatchShape = RoundedCornerShape(percent = 25)

@Composable
fun LegendSwatch(colour: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(LegendSwatchShape)
            .background(colour)
    )
}

@Preview(widthDp = 220)
@Composable
fun ActivityLegendPreview() {
    // Narrow on purpose, so the preview shows the entries wrapping onto a second line.
    val activities = listOf("Gym", "Sleeping", "Work", "Social", "Reading").mapIndexed { i, name ->
        Activity(name, LightActivityColours[i])
    }

    ActivityLegend(activityList = activities)
}
