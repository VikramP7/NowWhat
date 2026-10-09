package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.UnloggedColour
import kotlin.math.sin

/**
 * One line on a LineChart: a value per x position, between 0 and the chart's yMax.
 * A null is "nothing to measure here" and breaks the line; it is not the same as 0f,
 * which is "measured, and none". (Lives here until a second chart needs it, then moves
 * to its own file the way ChartSlice did.)
 */
data class ChartSeries(
    val label: String,
    val colour: Color,
    val values: List<Float?>
)

/**
 * Lines across evenly spaced x positions, each value placed between 0 on the bottom edge
 * and yMax on the top. Generic like the other charts: it knows nothing about weeks,
 * activities or percentages; the caller picks yMax, the gridlines and what the labels say.
 *
 * The first series is drawn last, so it sits on top where lines cross. Put the series the
 * reader cares most about first (activities before Unlogged).
 *
 * @param labelColour passed in because the Canvas lambda can't read TextColour itself
 *   (same rule as SleepBarChart and StackedColumnChart). Gridlines use a faint version of it.
 * @param xLabels optional text under the points, matched by index; null draws nothing.
 *   A label that would overlap the one before it is skipped, so a caller can't crowd the axis
 *   however long the history grows.
 * @param yMax the value at the top edge. Values and gridline positions use the same units,
 *   so a caller that fits yMax to its data passes gridlines up to that same top.
 * @param gridLines horizontal guides as (value, label) pairs, labels in a left gutter.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    labelColour: Color,
    modifier: Modifier = Modifier,
    xLabels: List<String?> = emptyList(),
    gridLines: List<Pair<Float, String>> = emptyList(),
    yMax: Float = 1f,
    chartHeight: Dp = 200.dp,
    lineWidth: Dp = 2.5.dp
) {
    // Measure here, where MaterialTheme is readable; draw the results inside the Canvas.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColour)
    val measuredX = xLabels.map { label -> label?.let { measurer.measure(it, labelStyle) } }
    val measuredGrid = gridLines.map { (value, label) -> value to measurer.measure(label, labelStyle) }
    val gridColour = labelColour.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        // The x axis is however many points the longest series has. Series of different
        // lengths still line up from the left, because x comes from the index alone.
        val pointCount = series.maxOfOrNull { it.values.size } ?: 0
        if (pointCount == 0 || yMax <= 0f) return@Canvas

        val stroke = lineWidth.toPx()
        val dotRadius = stroke
        val gap = 6.dp.toPx()

        val gridLabelWidth = measuredGrid.maxOfOrNull { it.second.size.width } ?: 0
        val gridLabelHeight = measuredGrid.maxOfOrNull { it.second.size.height } ?: 0
        val xLabelHeight = measuredX.filterNotNull().maxOfOrNull { it.size.height }

        // The plot rectangle. The left gutter holds the gridline labels; the bottom gutter holds
        // the x labels. Top and bottom also leave room for a dot on either edge, and for the half
        // of the top and bottom gridline labels that sits above or below its line.
        val edgeRoom = maxOf(dotRadius, gridLabelHeight / 2f)
        val plotLeft = if (measuredGrid.isEmpty()) 0f else gridLabelWidth + gap
        val plotRight = size.width
        val plotTop = edgeRoom
        val plotBottom = size.height - if (xLabelHeight != null) xLabelHeight + gap else edgeRoom
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop
        if (plotWidth <= 2 * dotRadius || plotHeight <= 0f) return@Canvas

        // Points are inset by the dot radius inside the plot, so the first and last dots don't
        // poke out over the gutter or the card edge. A single point sits in the middle instead
        // of dividing by zero.
        val firstX = plotLeft + dotRadius
        val lastX = plotRight - dotRadius
        fun xAt(index: Int): Float =
            if (pointCount == 1) (firstX + lastX) / 2f
            else firstX + index * (lastX - firstX) / (pointCount - 1)

        // Values go up, canvas y grows down: the flip lives here, as in the rhythm's yAt.
        // Dividing by yMax is the whole of the scaling; the clamp only matters if a caller
        // passes a value above its own top.
        fun yAt(value: Float): Float = plotBottom - (value / yMax).coerceIn(0f, 1f) * plotHeight

        // --- gridlines, under everything else ---
        measuredGrid.forEach { (value, label) ->
            val y = yAt(value)
            drawLine(
                color = gridColour,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx()
            )
            // Right-aligned in the gutter and centred on its line.
            drawText(
                textLayoutResult = label,
                topLeft = Offset(plotLeft - gap - label.size.width, y - label.size.height / 2f)
            )
        }

        // --- lines ---
        val lineStyle = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

        series.asReversed().forEach { line ->
            // One Path per series. moveTo lifts the pen and lineTo draws, so a null value
            // just means "lift the pen": the next real point starts a fresh segment.
            val path = Path()
            var penDown = false

            line.values.forEachIndexed { index, value ->
                if (value == null) {
                    penDown = false
                    return@forEachIndexed
                }
                val point = Offset(xAt(index), yAt(value))
                if (penDown) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                penDown = true

                // A point with a gap (or the edge) on both sides would be a line of zero length,
                // which draws nothing. Give it a dot so a one-week history is still visible.
                val noLineIn = line.values.getOrNull(index - 1) == null
                val noLineOut = line.values.getOrNull(index + 1) == null
                if (noLineIn && noLineOut) drawCircle(color = line.colour, radius = dotRadius, center = point)
            }

            drawPath(path = path, color = line.colour, style = lineStyle)
        }

        // --- x labels ---
        // Centred under their point and kept between the gutter and the canvas edge, then drawn
        // left to right, skipping any that would touch the last one drawn.
        var lastLabelRight = Float.NEGATIVE_INFINITY
        measuredX.forEachIndexed { index, label ->
            if (label == null || index >= pointCount) return@forEachIndexed

            val maxLeft = (size.width - label.size.width).coerceAtLeast(plotLeft)
            val left = (xAt(index) - label.size.width / 2f).coerceIn(plotLeft, maxLeft)
            if (left < lastLabelRight + gap) return@forEachIndexed

            drawText(textLayoutResult = label, topLeft = Offset(left, plotBottom + gap))
            lastLabelRight = left + label.size.width
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LineChartPreview() {
    val weeks = 12
    // Last week null for every series: the "Weekends on a Wednesday" gap at the right end.
    fun wobble(base: Float, amount: Float, phase: Float) =
        List(weeks) { i -> if (i == weeks - 1) null else base + amount * sin(i * 0.8f + phase) }

    LineChart(
        series = listOf(
            ChartSeries("Work", Color(LightActivityColours[0]), wobble(0.22f, 0.04f, 0f)),
            ChartSeries("Sleep", Color(LightActivityColours[1]), wobble(0.32f, 0.02f, 1f)),
            ChartSeries("Gym", Color(LightActivityColours[2]), wobble(0.04f, 0.01f, 2f)),
            ChartSeries("Unlogged", UnloggedColour, List(weeks) { i -> if (i == weeks - 1) null else 0.6f - i * 0.025f })
        ),
        labelColour = Color.DarkGray,
        xLabels = List(weeks) { i -> listOf("Jul 2026", null, null, null, "Aug", null, null, null, "Sep", null, null, null)[i] },
        gridLines = listOf(0f to "0%", 0.2f to "20%", 0.4f to "40%", 0.6f to "60%"),
        yMax = 0.6f
    )
}
