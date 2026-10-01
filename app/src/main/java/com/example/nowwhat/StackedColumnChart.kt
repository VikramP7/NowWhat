package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
 */
@Composable
fun StackedColumnChart(
    columns: List<List<DonutSlice>>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
    columnWidthFraction: Float = 0.72f,
    cornerRadius: Dp = 3.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        if (columns.isEmpty()) return@Canvas

        // Named plot edges, even though 7b has no labels: 7c reserves a gutter for the hour
        // labels by moving plotBottom up, and nothing else in here has to change.
        val plotTop = 0f
        val plotBottom = size.height
        val plotHeight = plotBottom - plotTop

        // Fixed slots: the column count sets the slot width, so an empty column still
        // occupies its slot.
        val slotWidth = size.width / columns.size
        val columnWidth = slotWidth * columnWidthFraction
        val radius = CornerRadius(cornerRadius.toPx())

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
            DonutSlice("Work", work, if (hour in 3..11) 5 else 1),
            DonutSlice("Sleep", sleep, if (hour >= 16) 6 else 1),
            DonutSlice("Unlogged", UnloggedColour, 2)
        )
    }
    StackedColumnChart(columns = columns)
}
