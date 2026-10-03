package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.UnloggedColour
import kotlin.math.roundToInt

/**
 * Stacked columns, each normalised to the full height: a column's segments are shares of
 * that column's own total, so every non-empty column is the same height and only the
 * proportions differ.
 *
 * Generic on purpose, like ActivityMatrixGrid: it knows nothing about hours of the day or
 * activities. The first slice of each column sits on the baseline, so the caller's order
 * decides what lines up along the bottom.
 *
 * @param columns one list of slices per column, left to right. A column whose hours sum
 *   to zero leaves its slot empty rather than shifting its neighbours along.
 * @param labelColour passed in rather than read from the theme, because the Canvas lambda is
 *   not a composable and can't read TextColour itself (same rule as SleepBarChart).
 * @param labels optional text under the columns, matched to them by index; a null entry
 *   draws no label. The chart doesn't choose which columns get one: the caller does.
 */
@Composable
fun StackedColumnChart(
    columns: List<List<ChartSlice>>,
    labelColour: Color,
    modifier: Modifier = Modifier,
    labels: List<String?> = emptyList(),
    chartHeight: Dp = 200.dp,
    columnWidthFraction: Float = 0.72f,
    cornerRadius: Dp = 3.dp
) {
    // Text on a Canvas is measure-then-draw, as in SleepBarChart: measure here, where
    // MaterialTheme is readable, and draw the results inside the Canvas lambda.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColour)
    val measuredLabels = labels.map { label -> label?.let { measurer.measure(it, labelStyle) } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        if (columns.isEmpty()) return@Canvas

        val gap = 6.dp.toPx()

        // The label gutter comes off the bottom of the plot, and only when there is something
        // to put in it, so a caller with no labels still gets the full height for the columns.
        val labelHeight = measuredLabels.filterNotNull().maxOfOrNull { it.size.height }
        val plotTop = 0f
        val plotBottom = if (labelHeight != null) size.height - labelHeight - gap else size.height
        val plotHeight = plotBottom - plotTop
        if (plotHeight <= 0f) return@Canvas // squeezed shorter than its own labels

        // Fixed slots: the column count sets the slot width, so an empty column still
        // occupies its slot.
        val slotWidth = size.width / columns.size
        val columnWidth = slotWidth * columnWidthFraction
        val radius = CornerRadius(cornerRadius.toPx())

        // --- columns ---
        columns.forEachIndexed { index, slices ->
            val total = slices.sumOf { it.hours }
            if (total <= 0) return@forEachIndexed // empty slot; same guard as the donut's

            val left = index * slotWidth + (slotWidth - columnWidth) / 2f
            val right = left + columnWidth

            // Pixel y of a boundary with `hoursBelow` hours stacked under it. Each boundary is
            // computed from the running total, not by adding segment heights together, so
            // rounding can never drift and the last segment always ends exactly at plotTop.
            // Rounding to whole pixels means neighbouring segments share an exact edge,
            // so no hairline of card background shows through the seam.
            fun yAt(hoursBelow: Int): Float =
                (plotBottom - hoursBelow.toFloat() / total * plotHeight).roundToInt().toFloat()

            // Clip once to the whole column's rounded outline, then fill it with plain
            // rectangles. Rounding each segment instead would put rounded corners at every
            // seam and the column would read as a stack of beads.
            val outline = Path().apply {
                addRoundRect(RoundRect(left, plotTop, right, plotBottom, radius))
            }
            clipPath(outline) {
                var hoursBelow = 0
                slices.forEach { slice ->
                    val bottom = yAt(hoursBelow)
                    hoursBelow += slice.hours
                    val top = yAt(hoursBelow)
                    drawRect(
                        color = slice.colour,
                        topLeft = Offset(left, top),
                        size = Size(columnWidth, bottom - top)
                    )
                }
            }
        }

        // --- labels ---
        // A separate loop from the columns, because an empty column still gets its label
        // (the column loop skips empty slots before it would reach any label drawing).
        measuredLabels.forEachIndexed { index, label ->
            if (label == null || index >= columns.size) return@forEachIndexed

            // Centred under its column, then nudged inward if that would run off either edge:
            // the first column's label is wider than half a slot, so centring alone clips it.
            // maxLeft is floored at 0 because coerceIn throws if its upper bound is below its
            // lower one, which a label wider than the whole chart would otherwise cause.
            val centre = index * slotWidth + slotWidth / 2f
            val maxLeft = (size.width - label.size.width).coerceAtLeast(0f)
            val x = (centre - label.size.width / 2f).coerceIn(0f, maxLeft)
            drawText(textLayoutResult = label, topLeft = Offset(x, plotBottom + gap))
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StackedColumnChartPreview() {
    val work = Color(LightActivityColours[0])
    val sleep = Color(LightActivityColours[1])
    val columns = List(24) { hour ->
        if (hour >= 20) emptyList() // later today: hasn't happened yet, so the slot stays empty
        else listOf(
            ChartSlice("Work", work, if (hour in 3..11) 5 else 1),
            ChartSlice("Sleep", sleep, if (hour >= 16) 6 else 1),
            ChartSlice("Unlogged", UnloggedColour, 2)
        )
    }
    val bandLabels = listOf("6am", "12pm", "6pm", "12am")
    StackedColumnChart(
        columns = columns,
        labels = List(24) { hour -> if (hour % 6 == 0) bandLabels[hour / 6] else null },
        labelColour = Color.DarkGray
    )
}
