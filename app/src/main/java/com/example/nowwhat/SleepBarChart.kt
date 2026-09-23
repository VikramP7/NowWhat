package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import kotlin.math.ceil
import kotlin.math.floor

// The vertical range of the chart, in SleepAverage offset units (hours after the noon pivot).
// Rounded out to whole hours so chunk 6b's gridlines land on the range edges.
data class SleepChartRange(val topOffset: Float, val bottomOffset: Float)

// null when there is nothing to draw: no range can be fitted to no data.
fun sleepChartRange(nights: Map<DayOfWeek, SleepAverage?>): SleepChartRange? {
    val averages = nights.values.filterNotNull()
    if (averages.isEmpty()) return null

    val top = floor(averages.minOf { it.bedOffset })
    val bottom = ceil(averages.maxOf { it.wakeOffset })
    return if (bottom > top) SleepChartRange(top, bottom) else null
}

/**
 * One floating bar per weekday: the top is that weekday's average bedtime, the bottom its
 * average waketime, so the bar's height is the average duration.
 *
 * Offsets grow later-into-the-night, and canvas y grows downward, so the mapping from offset
 * to y needs no flip.
 */
@Composable
fun SleepBarChart(
    nights: Map<DayOfWeek, SleepAverage?>,
    barColour: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 220.dp,
    barWidthFraction: Float = 0.6f,
    cornerRadius: Dp = 4.dp
) {
    // Nothing to draw and no range to draw it in; the caller shows an empty state instead.
    val range = sleepChartRange(nights) ?: return
    val span = range.bottomOffset - range.topOffset

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val slotWidth = size.width / DayOfWeek.entries.size
        val barWidth = slotWidth * barWidthFraction
        val barInset = (slotWidth - barWidth) / 2f
        val radius = CornerRadius(cornerRadius.toPx())

        fun yFor(offset: Float): Float = (offset - range.topOffset) / span * size.height

        // Iterate the weekdays, not the map: the axis is fixed Monday-to-Sunday whether or not
        // a given night has data, and a weekday missing from the map just draws nothing.
        DayOfWeek.entries.forEachIndexed { index, day ->
            val average = nights[day] ?: return@forEachIndexed

            val top = yFor(average.bedOffset)
            val bottom = yFor(average.wakeOffset)

            drawRoundRect(
                color = barColour,
                topLeft = Offset(x = index * slotWidth + barInset, y = top),
                size = Size(width = barWidth, height = bottom - top),
                cornerRadius = radius
            )
        }
    }
}
