package com.example.nowwhat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle as DayNameStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

// The vertical range of the chart, in SleepAverage offset units (hours after the noon pivot).
// Rounded out to whole hours so the gridlines land on the range edges.
data class SleepChartRange(val topOffset: Float, val bottomOffset: Float)

// null when there is nothing to draw: no range can be fitted to no data.
// Both maps feed in, so a planned bar with no actual sleep still fits inside the range.
fun sleepChartRange(
    nights: Map<DayOfWeek, SleepAverage?>,
    plannedNights: Map<DayOfWeek, SleepAverage?> = emptyMap()
): SleepChartRange? {
    val averages = nights.values.filterNotNull() + plannedNights.values.filterNotNull()
    if (averages.isEmpty()) return null

    val top = floor(averages.minOf { it.bedOffset })
    val bottom = ceil(averages.maxOf { it.wakeOffset })
    return if (bottom > top) SleepChartRange(top, bottom) else null
}

// Whole-hour offsets to label, thinned out so the axis never crowds: hourly for a short
// range, every two or three hours for a long one.
private fun sleepChartTicks(range: SleepChartRange): List<Float> {
    val span = range.bottomOffset - range.topOffset
    val step = when {
        span <= 8f -> 1
        span <= 16f -> 2
        else -> 3
    }
    val first = ceil(range.topOffset).toInt()
    val last = floor(range.bottomOffset).toInt()
    return (first..last).filter { (it - first) % step == 0 }.map { it.toFloat() }
}

/**
 * One floating bar per weekday: the top is that weekday's average bedtime, the bottom its
 * average waketime, so the bar's height is the average duration.
 *
 * Offsets grow later-into-the-night and canvas y grows downward, so the offset-to-y mapping
 * needs no flip.
 *
 * @param nights actual sleep per weekday; a null entry draws no bar but keeps its slot.
 * @param plannedNights planned sleep per weekday, drawn as a faint wide bar behind the actual one.
 * @param average the dashed bedtime/waketime lines. Pass the day-type-filtered average.
 * @param plannedAverage optional second pair of dashed lines, fainter.
 */
@Composable
fun SleepBarChart(
    nights: Map<DayOfWeek, SleepAverage?>,
    barColour: Color,
    labelColour: Color,
    is24Hour: Boolean,
    modifier: Modifier = Modifier,
    plannedNights: Map<DayOfWeek, SleepAverage?> = emptyMap(),
    average: SleepAverage? = null,
    plannedAverage: SleepAverage? = null,
    chartHeight: Dp = 240.dp,
    barWidthFraction: Float = 0.42f,
    plannedWidthFraction: Float = 0.72f,
    plannedAlpha: Float = 0.28f,
    cornerRadius: Dp = 4.dp
) {
    // Nothing to draw and no range to draw it in; the caller shows an empty state instead.
    val range = sleepChartRange(nights, plannedNights) ?: return
    val span = range.bottomOffset - range.topOffset

    // Text on a Canvas is measured first, then drawn at an exact offset. Measuring here (rather
    // than inside the draw lambda) also gives the label sizes that decide the axis gutters.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColour)

    val ticks = sleepChartTicks(range)
    val tickLabels = ticks.map { offset ->
        measurer.measure(formatClockLabel(sleepOffsetToMinuteOfDay(offset), is24Hour), labelStyle)
    }
    val dayLabels = DayOfWeek.entries.map { day ->
        measurer.measure(day.getDisplayName(DayNameStyle.SHORT, Locale.getDefault()), labelStyle)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val gap = 6.dp.toPx()

        // The plot area is what's left after the axis gutters. Half a label's height of
        // headroom top and bottom keeps the first and last y labels from clipping.
        val labelHeight = tickLabels.maxOf { it.size.height }.toFloat()
        val plotLeft = tickLabels.maxOf { it.size.width }.toFloat() + gap
        val plotRight = size.width
        val plotTop = labelHeight / 2f
        val plotBottom = size.height - dayLabels.maxOf { it.size.height } - gap - labelHeight / 2f
        val plotHeight = plotBottom - plotTop
        if (plotHeight <= 0f || plotRight <= plotLeft) return@Canvas

        fun yFor(offset: Float): Float = plotTop + (offset - range.topOffset) / span * plotHeight

        val slotWidth = (plotRight - plotLeft) / DayOfWeek.entries.size
        val barWidth = slotWidth * barWidthFraction
        val plannedWidth = slotWidth * plannedWidthFraction
        val radius = CornerRadius(cornerRadius.toPx())

        // --- gridlines and y labels ---
        ticks.forEachIndexed { index, offset ->
            val y = yFor(offset)
            drawLine(
                color = labelColour.copy(alpha = 0.18f),
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx()
            )
            val label = tickLabels[index]
            drawText(
                textLayoutResult = label,
                topLeft = Offset(plotLeft - gap - label.size.width, y - label.size.height / 2f)
            )
        }

        // --- bars ---
        // Iterate the weekdays, not the map: the seven slots are fixed whatever the map holds,
        // so a weekday with no data leaves a gap instead of shifting its neighbours along.
        DayOfWeek.entries.forEachIndexed { index, day ->
            val slotCentre = plotLeft + index * slotWidth + slotWidth / 2f

            plannedNights[day]?.let { planned ->
                val top = yFor(planned.bedOffset)
                drawRoundRect(
                    color = barColour.copy(alpha = plannedAlpha),
                    topLeft = Offset(slotCentre - plannedWidth / 2f, top),
                    size = Size(plannedWidth, yFor(planned.wakeOffset) - top),
                    cornerRadius = radius
                )
            }

            nights[day]?.let { actual ->
                val top = yFor(actual.bedOffset)
                drawRoundRect(
                    color = barColour,
                    topLeft = Offset(slotCentre - barWidth / 2f, top),
                    size = Size(barWidth, yFor(actual.wakeOffset) - top),
                    cornerRadius = radius
                )
            }

            val label = dayLabels[index]
            drawText(
                textLayoutResult = label,
                topLeft = Offset(slotCentre - label.size.width / 2f, plotBottom + gap)
            )
        }

        // --- dashed average lines, drawn last so they read on top of the bars ---
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))

        fun drawAverageLines(value: SleepAverage, colour: Color) {
            listOf(value.bedOffset, value.wakeOffset).forEach { offset ->
                val y = yFor(offset)
                drawLine(
                    color = colour,
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dash
                )
            }
        }

        plannedAverage?.let { drawAverageLines(it, labelColour.copy(alpha = 0.35f)) }
        average?.let { drawAverageLines(it, labelColour.copy(alpha = 0.75f)) }
    }
}
