package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.TextColour

// The ghost's transparency, shared by the chart and its key so the two can't drift apart.
// (SleepBarChart's plannedAlpha is the same 0.28f, kept separately in that file.)
private const val GHOST_ALPHA = 0.28f

// One row of the chart. Generic on purpose: "ghost" and "solid" describe how each value is
// drawn, not what it means. The screen decides that ghost = planned and solid = actual.
data class PairedBar(
    val label: String,
    val colour: Color,
    val ghost: Float,               // drawn wide and faint, behind
    val solid: Float,               // drawn narrow and opaque, in front
    val valueText: String? = null   // optional text at the row's right end, formatted by the caller
)

// Horizontal paired bars on one shared scale. The sleep chart's vocabulary turned sideways:
// the ghost is the activity's own colour at low alpha, the solid bar sits on top of it, so a
// solid bar falling short of its ghost reads as "under" and one running past it as "over".
//
// A Compose layout rather than a Canvas, for the same reason as ActivityMatrixGrid: every row
// holds text, and Text already knows how to ellipsise itself.
@Composable
fun PairedBarChart(
    bars: List<PairedBar>,
    modifier: Modifier = Modifier,
    scaleMax: Float? = null,        // null = fit to the largest value in any row
    maxLabelWidth: Dp = 120.dp,     // cap, so one long activity name can't starve every track
    columnGap: Dp = 8.dp,           // between name, track and value
    rowGap: Dp = 10.dp,
    ghostThickness: Dp = 20.dp,
    solidThickness: Dp = 10.dp,
    ghostAlpha: Float = GHOST_ALPHA,
    cornerRadius: Dp = 4.dp,        // matches SleepBarChart's bars
    minBarWidth: Dp = 3.dp          // so 1 hour out of 2,000 is still a visible sliver
) {
    val labelStyle = MaterialTheme.typography.bodyMedium
    val valueStyle = MaterialTheme.typography.bodySmall

    // The side columns are sized to their widest text, measured up front: the same
    // measure-then-use idea as SleepBarChart's gutters, except here the result sizes a
    // Compose layout instead of a draw call. Every row gets the same two widths, so every
    // track is the same width too, and that is what keeps the scale shared across rows.
    // remember() so the measuring only reruns when the bars (or the text style) change.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val (labelWidth, valueWidth) = remember(bars, labelStyle, valueStyle, density) {
        fun widest(texts: List<String>, style: TextStyle): Dp = with(density) {
            (texts.maxOfOrNull { measurer.measure(it, style, maxLines = 1).size.width } ?: 0).toDp()
        }
        widest(bars.map { it.label }, labelStyle).coerceAtMost(maxLabelWidth) to
            widest(bars.mapNotNull { it.valueText }, valueStyle)
    }
    val showValues = bars.any { it.valueText != null }

    // The shared scale: every bar in every row is a fraction of this one number. If it's zero
    // there is nothing to draw, and dividing by it would make every fraction NaN.
    val top = scaleMax ?: bars.maxOfOrNull { maxOf(it.ghost, it.solid) } ?: 0f
    if (!(top > 0f)) return  // written this way round so NaN is caught too (NaN > 0f is false)

    val shape = RoundedCornerShape(cornerRadius)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(rowGap)
    ) {
        bars.forEach { bar ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(columnGap)
            ) {
                // Name column: exactly as wide as the widest name (up to the cap), so a
                // short name no longer leaves a slab of empty space before its bar.
                Text(
                    text = bar.label,
                    style = labelStyle,
                    color = TextColour,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(labelWidth)
                )

                // The track takes whatever is left. Both bars start at its left edge
                // (CenterStart), and the solid one is composed second, so it draws on top:
                // painter's algorithm, as in the sleep chart.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ghostThickness),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BarSegment(
                        fraction = bar.ghost / top,
                        thickness = ghostThickness,
                        colour = bar.colour.copy(alpha = ghostAlpha),
                        shape = shape,
                        minWidth = minBarWidth
                    )
                    BarSegment(
                        fraction = bar.solid / top,
                        thickness = solidThickness,
                        colour = bar.colour,
                        shape = shape,
                        minWidth = minBarWidth
                    )
                }

                // Value column: as wide as the widest value, right-aligned so the digits line up.
                // Reserved on every row or on none, because a row without it would get a wider
                // track and the shared scale would quietly stop being shared.
                if (showValues) {
                    Text(
                        text = bar.valueText.orEmpty(),
                        style = valueStyle,
                        color = TextColour,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(valueWidth)
                    )
                }
            }
        }
    }
}

@Composable
private fun BarSegment(
    fraction: Float,
    thickness: Dp,
    colour: Color,
    shape: RoundedCornerShape,
    minWidth: Dp
) {
    // Zero means "none": draw nothing rather than a minimum-width sliver that would read as
    // "a little". Same NaN-safe shape as the guard above.
    if (!(fraction > 0f)) return

    Box(
        modifier = Modifier
            // Order matters. widthIn sets a floor on the incoming constraints, and fillMaxWidth's
            // width is then coerced into them, so the result is max(minWidth, fraction * track).
            // The other way round, fillMaxWidth would fix the width first and widthIn couldn't raise it.
            .widthIn(min = minWidth)
            // fillMaxWidth throws for a fraction above 1, which a caller-supplied scaleMax smaller
            // than the data would produce. Clamp so that case overflows nothing instead of crashing.
            .fillMaxWidth(fraction.coerceAtMost(1f))
            .height(thickness)
            .clip(shape)
            .background(colour)
    )
}

// The key for the chart: what the faint bar and the solid bar mean. It draws the two shapes in
// TextColour rather than an activity colour, because the key is about the *treatment* (faint and
// thick vs solid and thin) and every row already says which activity it is. The labels are
// parameters, so the meaning still lives at the call site.
// Spacing and text style match SwatchLegend, so the two read as the same kind of thing.
@Composable
fun PairedBarKey(
    ghostLabel: String,
    solidLabel: String,
    modifier: Modifier = Modifier,
    ghostAlpha: Float = GHOST_ALPHA
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Same 2:1 thickness ratio as the chart's 20dp ghost and 10dp solid bar.
        KeyEntry(label = ghostLabel, swatchHeight = 12.dp, colour = TextColour.copy(alpha = ghostAlpha))
        KeyEntry(label = solidLabel, swatchHeight = 6.dp, colour = TextColour)
    }
}

@Composable
private fun KeyEntry(label: String, swatchHeight: Dp, colour: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = swatchHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(colour)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextColour
        )
    }
}
