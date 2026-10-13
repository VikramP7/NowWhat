package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.UnloggedColour
import kotlin.math.abs
import kotlin.math.roundToInt
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
 * Drawn as two Canvases side by side: a fixed gutter holding the gridline labels, and the plot,
 * which scrolls sideways once there are more points than [visiblePoints]. Both work out their
 * vertical positions with the same yAt, so the labels stay level with their lines.
 *
 * @param labelColour passed in because the Canvas lambda can't read TextColour itself
 *   (same rule as SleepBarChart and StackedColumnChart). Gridlines use a faint version of it.
 * @param xLabels optional text under the points, matched by index; null draws nothing.
 *   A label that would overlap the one before it is skipped, so a caller can't crowd the axis
 *   however long the history grows.
 * @param yMax the value at the top edge. Values and gridline positions use the same units,
 *   so a caller that fits yMax to its data passes gridlines up to that same top. The scale
 *   covers every point, not just the visible ones, so scrolling never rescales the chart.
 * @param gridLines horizontal guides as (value, label) pairs, labels in a left gutter.
 * @param visiblePoints how many points fit across the visible width before the plot scrolls.
 *   It opens on the newest (rightmost) points. Null, or no more points than this, spreads every
 *   point across the width with no scrolling, as before.
 * @param selectedLabel the series to highlight, by label: drawn on top and thicker, every other
 *   series faded. Null, or a label no series has, draws every line normally.
 * @param selectedIndex the x position to mark on the selected series: a faint vertical marker
 *   and a larger dot. Ignored without a selected series. Out of range draws no marker.
 * @param onTap called for a tap on the plot with the nearest x index and the label of the series
 *   closest to the finger there, or null for the label when no line is within [tapRadius]. What
 *   that means (select, move the marker, clear) is the caller's decision; the chart owns nothing.
 * @param tapRadius how close, vertically, a tap must be to a line to count as touching it.
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
    lineWidth: Dp = 2.5.dp,
    visiblePoints: Int? = null,
    selectedLabel: String? = null,
    selectedIndex: Int? = null,
    onTap: (label: String?, index: Int) -> Unit = { _, _ -> },
    tapRadius: Dp = 24.dp,
    fadedAlpha: Float = 0.25f
) {
    // Starts at Int.MAX_VALUE so the very first frame is already scrolled to the newest points:
    // ScrollState clamps its value down to maxValue as soon as the layout reports one.
    val scrollState = rememberScrollState(initial = Int.MAX_VALUE)

    // pointerInput only restarts when its keys change, so it reads the latest onTap through this
    // rather than keeping the first lambda it captured (same as DonutChart's onSliceClick).
    val currentOnTap by rememberUpdatedState(onTap)

    // The x axis is however many points the longest series has. Series of different
    // lengths still line up from the left, because x comes from the index alone.
    val pointCount = series.maxOfOrNull { it.values.size } ?: 0
    if (pointCount == 0 || yMax <= 0f) {
        Spacer(modifier.fillMaxWidth().height(chartHeight))
        return
    }

    // Measure here, where MaterialTheme is readable; draw the results inside the Canvases.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColour)
    val measuredX = xLabels.map { label -> label?.let { measurer.measure(it, labelStyle) } }
    val measuredGrid = gridLines.map { (value, label) -> value to measurer.measure(label, labelStyle) }
    val gridColour = labelColour.copy(alpha = 0.15f)

    // Sizes both Canvases share, in pixels. Worked out once out here (outside a Canvas there's
    // no toPx(), hence LocalDensity) so the gutter and the plot can't disagree about them.
    val density = LocalDensity.current
    val stroke = with(density) { lineWidth.toPx() }
    val dotRadius = stroke
    val gap = with(density) { 6.dp.toPx() }

    val gridLabelWidth = measuredGrid.maxOfOrNull { it.second.size.width } ?: 0
    val gridLabelHeight = measuredGrid.maxOfOrNull { it.second.size.height } ?: 0
    val xLabelHeight = measuredX.filterNotNull().maxOfOrNull { it.size.height }

    // Top and bottom leave room for a dot on either edge, and for the half of the top and
    // bottom gridline labels that sits above or below its line. The bottom also holds x labels.
    val topRoom = maxOf(dotRadius, gridLabelHeight / 2f)
    val bottomRoom = if (xLabelHeight != null) xLabelHeight + gap else topRoom

    // Values go up, canvas y grows down: the flip lives here, as in the rhythm's yAt.
    // Dividing by yMax is the whole of the scaling; the clamp only matters if a caller
    // passes a value above its own top. Takes the height because each Canvas passes its own
    // (they're the same, both fill chartHeight).
    fun yAt(value: Float, height: Float): Float {
        val plotBottom = height - bottomRoom
        val plotHeight = (plotBottom - topRoom).coerceAtLeast(0f)
        return plotBottom - (value / yMax).coerceIn(0f, 1f) * plotHeight
    }

    // Points are inset by the dot radius at both ends, so the first and last dots don't poke out
    // past the plot's edges. A single point sits in the middle instead of dividing by zero.
    // Out here rather than inside the Canvas, so drawing and tap-testing share one rule.
    fun xAt(index: Int, width: Float): Float {
        val firstX = dotRadius
        val lastX = width - dotRadius
        return if (pointCount == 1) (firstX + lastX) / 2f
        else firstX + index * (lastX - firstX) / (pointCount - 1)
    }

    // The nearest x index to a horizontal position: xAt run backwards, rounded, kept on the axis.
    fun indexAt(x: Float, width: Float): Int {
        if (pointCount == 1) return 0
        val spacing = (width - 2 * dotRadius) / (pointCount - 1)
        return ((x - dotRadius) / spacing).roundToInt().coerceIn(0, pointCount - 1)
    }

    // The selection only counts if some series has that label, so a stale label can't fade every line.
    val active = selectedLabel?.takeIf { label -> series.any { it.label == label } }

    // Drawn back to front: the reversed series (so the first series ends up on top, as before),
    // then the selected series moved to the very end so it's drawn over everything.
    // sortedBy is stable, so the unselected lines keep their order.
    val drawOrder = series.asReversed().sortedBy { it.label == active }

    // The gutter is exactly as wide as the widest gridline label plus the gap before the plot.
    val gutterWidth = if (measuredGrid.isEmpty()) 0.dp else with(density) { (gridLabelWidth + gap).toDp() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        // --- gutter: gridline labels, fixed in place ---
        if (measuredGrid.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
            ) {
                measuredGrid.forEach { (value, label) ->
                    // Right-aligned against the gap and centred on its line.
                    drawText(
                        textLayoutResult = label,
                        topLeft = Offset(
                            size.width - gap - label.size.width,
                            yAt(value, size.height) - label.size.height / 2f
                        )
                    )
                }
            }
        }

        // --- plot: gridlines, lines and x labels, scrolling when it's wider than the view ---
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val visible = visiblePoints?.coerceAtLeast(2)

            // Points are inset by the dot radius at both ends, so the first and last dots don't
            // poke out past the edge. With room to spare, the points spread across the view as
            // before; with more than fit, the spacing is fixed at what `visible` points would
            // get, and the plot grows wider than the view to make room for the rest.
            val plotWidth =
                if (visible == null || pointCount <= visible) viewWidth
                else {
                    val spacing = (viewWidth - 2 * dotRadius) / (visible - 1)
                    2 * dotRadius + spacing * (pointCount - 1)
                }
            val plotWidthDp = with(density) { plotWidth.toDp() }

            // Jump to the newest points whenever the plot's width changes: more weeks, a filter
            // with a different history, a rotation. Scrolling back yourself doesn't change
            // maxValue, so it's left alone.
            LaunchedEffect(scrollState.maxValue) {
                scrollState.scrollTo(scrollState.maxValue)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(plotWidthDp)
                        .fillMaxHeight()
                        // A tap picks the nearest week, then the line nearest the finger at that
                        // week. Tap positions here are in the plot's own coordinates, scrolled
                        // part included, so the same xAt/yAt as the drawing apply directly.
                        // Taps and scrolls don't fight: detectTapGestures gives up on a drag.
                        .pointerInput(series, yMax, gridLines, xLabels) {
                            detectTapGestures { tap ->
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                val index = indexAt(tap.x, width)
                                val nearest = series
                                    .mapNotNull { line ->
                                        line.values.getOrNull(index)?.let { value ->
                                            line.label to abs(yAt(value, height) - tap.y)
                                        }
                                    }
                                    .minByOrNull { it.second }
                                    ?.takeIf { it.second <= tapRadius.toPx() }
                                currentOnTap(nearest?.first, index)
                            }
                        }
                ) {
                    if (size.width <= 2 * dotRadius) return@Canvas
                    fun pointX(index: Int): Float = xAt(index, size.width)

                    // --- gridlines, under everything else ---
                    measuredGrid.forEach { (value, _) ->
                        val y = yAt(value, size.height)
                        drawLine(
                            color = gridColour,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // --- lines ---
                    val lineStyle = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val selectedStyle = Stroke(width = stroke * 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)

                    drawOrder.forEach { line ->
                        // Each line is one Path drawn in one call, so fading it with a translucent
                        // colour is safe here: nothing is painted twice (unlike the donut's slices).
                        val isSelected = line.label == active
                        val colour = if (active == null || isSelected) line.colour else line.colour.copy(alpha = fadedAlpha)

                        // One Path per series. moveTo lifts the pen and lineTo draws, so a null value
                        // just means "lift the pen": the next real point starts a fresh segment.
                        val path = Path()
                        var penDown = false

                        line.values.forEachIndexed { index, value ->
                            if (value == null) {
                                penDown = false
                                return@forEachIndexed
                            }
                            val point = Offset(pointX(index), yAt(value, size.height))
                            if (penDown) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                            penDown = true

                            // A point with a gap (or the edge) on both sides would be a line of zero
                            // length, which draws nothing. Give it a dot so a one-week history shows.
                            val noLineIn = line.values.getOrNull(index - 1) == null
                            val noLineOut = line.values.getOrNull(index + 1) == null
                            if (noLineIn && noLineOut) drawCircle(color = colour, radius = dotRadius, center = point)
                        }

                        drawPath(path = path, color = colour, style = if (isSelected) selectedStyle else lineStyle)
                    }

                    // --- marker on the selected series ---
                    // A faint vertical line through the chosen x, then a larger dot where the
                    // selected series crosses it. No dot when the series has no value there.
                    val marked = active?.let { label -> series.firstOrNull { it.label == label } }
                    if (marked != null && selectedIndex != null && selectedIndex in 0 until pointCount) {
                        val x = pointX(selectedIndex)
                        drawLine(
                            color = labelColour.copy(alpha = 0.35f),
                            start = Offset(x, yAt(yMax, size.height)),
                            end = Offset(x, yAt(0f, size.height)),
                            strokeWidth = 1.dp.toPx()
                        )
                        marked.values.getOrNull(selectedIndex)?.let { value ->
                            drawCircle(
                                color = marked.colour,
                                radius = stroke * 2f,
                                center = Offset(x, yAt(value, size.height))
                            )
                        }
                    }

                    // --- x labels ---
                    // Centred under their point and kept inside the plot, then drawn left to right,
                    // skipping any that would touch the last one drawn. This runs across the whole
                    // plot, not just the visible part, so labels don't shuffle as you scroll.
                    val labelTop = size.height - bottomRoom + gap
                    var lastLabelRight = Float.NEGATIVE_INFINITY
                    measuredX.forEachIndexed { index, label ->
                        if (label == null || index >= pointCount) return@forEachIndexed

                        val maxLeft = (size.width - label.size.width).coerceAtLeast(0f)
                        val left = (pointX(index) - label.size.width / 2f).coerceIn(0f, maxLeft)
                        if (left < lastLabelRight + gap) return@forEachIndexed

                        drawText(textLayoutResult = label, topLeft = Offset(left, labelTop))
                        lastLabelRight = left + label.size.width
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LineChartPreview() {
    val weeks = 30
    fun wobble(base: Float, amount: Float, phase: Float) =
        List(weeks) { i -> base + amount * sin(i * 0.8f + phase) }
    val months = listOf("Apr 2026", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov")

    LineChart(
        series = listOf(
            ChartSeries("Work", Color(LightActivityColours[0]), wobble(0.22f, 0.04f, 0f)),
            ChartSeries("Sleep", Color(LightActivityColours[1]), wobble(0.32f, 0.02f, 1f)),
            ChartSeries("Gym", Color(LightActivityColours[2]), wobble(0.04f, 0.01f, 2f)),
            ChartSeries("Unlogged", UnloggedColour, List(weeks) { i -> 0.6f - i * 0.012f })
        ),
        labelColour = Color.DarkGray,
        xLabels = List(weeks) { i -> if (i % 4 == 0) months[i / 4] else null },
        gridLines = listOf(0f to "0%", 0.2f to "20%", 0.4f to "40%", 0.6f to "60%"),
        yMax = 0.6f,
        visiblePoints = 14,
        selectedLabel = "Work",
        selectedIndex = weeks - 3
    )
}
