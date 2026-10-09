package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.UnloggedColour
import kotlin.math.sin

/**
 * One line on a LineChart: a value per x position, as a fraction of the full height.
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
 * Lines across evenly spaced x positions, each value a fraction of the plot height:
 * 0f on the bottom edge, 1f on the top. Generic like the other charts: it knows nothing
 * about weeks or activities, and the caller decides what a fraction means.
 *
 * The first series is drawn last, so it sits on top where lines cross. Put the series the
 * reader cares most about first (activities before Unlogged).
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
    lineWidth: Dp = 2.5.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        // The x-axis is however many points the longest series has. Series of different
        // lengths still line up from the left, because x comes from the index alone.
        val pointCount = series.maxOfOrNull { it.values.size } ?: 0
        if (pointCount == 0) return@Canvas

        val stroke = lineWidth.toPx()
        val dotRadius = stroke

        // Named plot edges, inset by the dot radius so a point at 0%, 100% or either end
        // isn't cut in half by the canvas edge. 8d moves plotLeft and plotBottom in to make
        // room for the axis labels, the same way 7c did for the rhythm.
        val plotLeft = dotRadius
        val plotRight = size.width - dotRadius
        val plotTop = dotRadius
        val plotBottom = size.height - dotRadius
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop
        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        // A single point has no width to spread across, so it sits in the middle instead
        // of dividing by zero.
        fun xAt(index: Int): Float =
            if (pointCount == 1) plotLeft + plotWidth / 2f
            else plotLeft + index * plotWidth / (pointCount - 1)

        // Hours stack up, canvas y grows down: the flip lives here, as in the rhythm's yAt.
        fun yAt(fraction: Float): Float = plotBottom - fraction.coerceIn(0f, 1f) * plotHeight

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
        )
    )
}
