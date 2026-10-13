package com.example.nowwhat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.UnloggedColour
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A ring of slices, each slice's angle its share of the total hours, starting at 12 o'clock and
 * running clockwise. Slices have rounded corners and a gap between them, and the ring sweeps in
 * from 12 o'clock when it first appears.
 *
 * @param ringFraction ring thickness as a fraction of the donut's width, so the hole keeps its
 *   proportions on every screen size. 0.25 leaves a hole half the donut's width. Capped at 0.45:
 *   every slice is drawn from the rim to the hole, so there has to be a hole (a pie, 0.5, would
 *   leave every slice's edges meeting at a single point).
 * @param sliceGap the width of the gap between neighbouring slices. Constant along its length
 *   (the slice edges are offset in parallel), so it reads as a clean groove, not a wedge.
 * @param cornerRadius how rounded each slice's four corners are.
 * @param animationMillis how long the opening sweep takes; 0 draws the finished ring at once.
 * @param selectedLabel the slice to raise, by label; every other slice fades. Null, or a label no
 *   slice has (a filter change can do that), draws the ring normally. The chart only draws the
 *   selection; the caller owns it.
 * @param onSliceClick called with the tapped slice's label, or with null for a tap in the hole or
 *   outside the ring. What a tap *means* (select, toggle off, clear) is the caller's decision.
 * @param selectionLift how far the selected slice moves outward. The ring is drawn this much
 *   smaller than the box, always, so the raised slice never gets clipped.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    ringFraction: Float = 0.25f,
    sliceGap: Dp = 3.dp,
    cornerRadius: Dp = 4.dp,
    animationMillis: Int = 700,
    selectedLabel: String? = null,
    onSliceClick: (String?) -> Unit = {},
    selectionLift: Dp = 6.dp,
    dimAlpha: Float = 0.35f,
    centreContent: @Composable () -> Unit = {}
) {
    val total = slices.sumOf { it.hours }

    // The selection only counts if a slice with hours actually has that label. Without this, a
    // filter that removes the selected activity would leave every slice faded and none raised.
    val active = selectedLabel?.takeIf { label -> slices.any { it.label == label && it.hours > 0 } }

    // pointerInput below only restarts when its keys change, so a plain onSliceClick would leave it
    // calling whichever lambda it captured first. rememberUpdatedState hands it the latest one.
    val currentOnSliceClick by rememberUpdatedState(onSliceClick)

    // The opening sweep: 0 → 1, multiplied into every slice's angle so the ring grows clockwise
    // from 12 o'clock. Animatable + LaunchedEffect(Unit) is the one-shot form: it runs once when
    // the donut enters the composition, rather than on every change of a target value (which is
    // what animateFloatAsState is for). Previews start finished, because a static preview never
    // runs the LaunchedEffect and would otherwise show an empty ring.
    val startsFinished = animationMillis <= 0 || LocalInspectionMode.current
    val progress = remember { Animatable(if (startsFinished) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = animationMillis, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Taps: work out which slice is under the finger from its angle and its distance
                // from the centre. Keyed on what the lambda reads, so it restarts with new data
                // rather than hit-testing yesterday's slices.
                .pointerInput(slices, ringFraction, selectionLift) {
                    detectTapGestures { tap ->
                        val radii = ringRadii(size.width.toFloat(), size.height.toFloat(), ringFraction, selectionLift.toPx())
                        val dx = tap.x - size.width / 2f
                        val dy = tap.y - size.height / 2f
                        val distance = hypot(dx, dy)
                        if (total <= 0 || distance < radii.inner || distance > radii.outer + selectionLift.toPx()) {
                            currentOnSliceClick(null)
                            return@detectTapGestures
                        }
                        // atan2 measures from 3 o'clock; the slices start at 12, so turn it a
                        // quarter and wrap into 0..360 before walking the slices' running total.
                        val angle = ((atan2(dy, dx) * (180.0 / PI).toFloat()) + 90f + 360f) % 360f
                        var end = 0f
                        val hit = slices.firstOrNull { slice ->
                            end += 360f * slice.hours / total
                            slice.hours > 0 && angle < end
                        }
                        currentOnSliceClick(hit?.label)
                    }
                }
        ) {
            if (total <= 0) return@Canvas

            val centre = center
            val radii = ringRadii(size.width, size.height, ringFraction, selectionLift.toPx())
            val outerRadius = radii.outer
            val innerRadius = radii.inner
            val thickness = outerRadius - innerRadius

            // A corner can be at most half the ring's thickness, or the two rounded corners on
            // one side would overlap.
            val corner = cornerRadius.toPx().coerceIn(0f, thickness / 2f)
            val halfGap = sliceGap.toPx() / 2f

            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.hours / total * progress.value
                // With a selection, the selected slice moves outward along its own centre line
                // and keeps its colour; every other slice fades.
                val isSelected = slice.label == active
                val faded = active != null && !isSelected
                val sliceCentre = if (isSelected) {
                    val midRad = (startAngle + sweep / 2f) * (PI / 180.0).toFloat()
                    val lift = selectionLift.toPx()
                    Offset(centre.x + lift * cos(midRad), centre.y + lift * sin(midRad))
                } else centre
                if (sweep > 0f) {
                    if (sweep >= 359.99f) {
                        // One slice is the whole ring: no edges, so no gap and no corners.
                        drawCircle(
                            color = if (faded) slice.colour.copy(alpha = dimAlpha) else slice.colour,
                            radius = (outerRadius + innerRadius) / 2f,
                            center = centre,
                            style = Stroke(width = thickness)
                        )
                    } else {
                        // Every slice reaches all the way from the rim to the hole. A narrow slice
                        // gets a smaller corner radius than the rest, just small enough that its
                        // edges still reach the hole; one too narrow even for square corners (null)
                        // isn't drawn at all.
                        sliceCornerFor(sweep, innerRadius, halfGap, corner)?.let { sliceCorner ->
                            sliceOutline(
                                centre = sliceCentre,
                                startAngle = startAngle,
                                sweep = sweep,
                                outer = outerRadius - sliceCorner,
                                inner = innerRadius + sliceCorner,
                                edgeInset = sliceCorner + halfGap
                            )?.let { path ->
                                // The rounded-corner trick: the outline is the slice *shrunk* by its
                                // corner radius on every side. Fill it, then stroke it with a
                                // round-joined line twice the corner radius wide: the stroke grows
                                // the shape back out to its true edges, and the round joins turn
                                // every corner into an arc of exactly that radius.
                                //
                                // The fill and the stroke overlap along the shrunk outline. Drawn at
                                // full opacity that's invisible, but drawn translucent the overlap
                                // band is painted twice and shows as a darker line inside the edge.
                                // So a faded slice is drawn opaque into its own layer, and the layer
                                // is faded as a whole when it's restored: the alpha applies once, to
                                // the finished shape.
                                if (faded) drawIntoCanvas { canvas ->
                                    canvas.saveLayer(path.getBounds().inflate(sliceCorner), Paint().apply { alpha = dimAlpha })
                                }
                                drawPath(path = path, color = slice.colour)
                                drawPath(
                                    path = path,
                                    color = slice.colour,
                                    style = Stroke(width = sliceCorner * 2f, join = StrokeJoin.Round)
                                )
                                if (faded) drawIntoCanvas { canvas -> canvas.restore() }
                            }
                        }
                    }
                }
                startAngle += sweep
            }
        }
        centreContent()
    }
}

// The ring's two radii for a box of this size. One function for drawing and for hit-testing taps,
// so a tap is measured against exactly the ring that was drawn. The outer edge is pulled in by the
// selection lift, so a raised slice still fits inside the box.
private data class RingRadii(val outer: Float, val inner: Float)

private fun ringRadii(width: Float, height: Float, ringFraction: Float, lift: Float): RingRadii {
    val diameter = minOf(width, height)
    val outer = (diameter / 2f - lift).coerceAtLeast(0f)
    val thickness = diameter * ringFraction.coerceIn(0f, 0.45f)
    return RingRadii(outer = outer, inner = (outer - thickness).coerceAtLeast(0f))
}

/**
 * The largest corner radius, up to [corner], that still lets a slice of this [sweep] reach the
 * hole, or null when even square corners won't (the slice is too narrow to draw).
 *
 * The slice's straight edges are inset in parallel by corner + halfGap, and two parallel-inset
 * edges cross at (corner + halfGap) / sin(half the sweep) from the centre. Requiring that to be
 * inside the shrunk inner radius, innerRadius + corner, and solving for the corner gives
 * (innerRadius * sin - halfGap) / (1 - sin). Big slices get the full corner; narrow ones get less
 * (their ends at the hole come out fully rounded); the narrowest, under about 0.75% of the ring on
 * a phone-width donut, can't fit the gap at the hole and aren't drawn.
 */
private fun sliceCornerFor(sweep: Float, innerRadius: Float, halfGap: Float, corner: Float): Float? {
    if (sweep >= 180f) return corner  // past half a turn the edges never cross on this side
    val s = sin(sweep / 2f * (PI / 180.0).toFloat())
    val fits = (innerRadius * s - halfGap) / (1f - s)
    if (fits <= 0f) return null
    // A hair under the exact fit, so the inner arc keeps a sliver of length rather than meeting
    // at a single point (which leaves asin() balanced on its domain edge).
    return minOf(corner, fits * 0.999f)
}

/**
 * The shrunk outline of one slice, or null when the slice is too thin to draw.
 *
 * Its two straight edges are moved inward by [edgeInset], parallel to the slice's original edges.
 * A point on a parallel line at radius r sits asin(inset / r) radians in from the original edge,
 * so the outer and inner arcs each get their own angular trim. The line between their ends is
 * then exactly parallel, which is what keeps the gap the same width from the hole to the rim.
 */
private fun sliceOutline(
    centre: Offset,
    startAngle: Float,
    sweep: Float,
    outer: Float,
    inner: Float,
    edgeInset: Float
): Path? {
    if (outer <= 0f || edgeInset >= inner) return null

    fun trimDegrees(radius: Float): Float = asin(edgeInset / radius) * (180.0 / PI).toFloat()
    val outerTrim = trimDegrees(outer)
    val innerTrim = trimDegrees(inner)
    // Safety net: sliceCornerFor already chose a corner that keeps this positive.
    if (sweep - 2f * innerTrim <= 0f) return null

    return Path().apply {
        // Outer arc, clockwise, trimmed at both ends.
        arcTo(
            rect = Rect(centre, outer),
            startAngleDegrees = startAngle + outerTrim,
            sweepAngleDegrees = sweep - 2f * outerTrim,
            forceMoveTo = true
        )
        // Inner arc, anticlockwise back to the start. Its first point comes from an implied line,
        // which is the slice's trailing straight edge.
        arcTo(
            rect = Rect(centre, inner),
            startAngleDegrees = startAngle + sweep - innerTrim,
            sweepAngleDegrees = -(sweep - 2f * innerTrim),
            forceMoveTo = false
        )
        close()  // the leading straight edge
    }
}

@Preview(showBackground = true)
@Composable
private fun DonutChartPreview() {
    // One large, a few medium, one under 2% (full depth, with a smaller corner) and a tiny one
    // (too thin to draw at all).
    val hours = listOf(320, 180, 90, 60, 15, 2)
    val slices = hours.mapIndexed { i, h ->
        ChartSlice("Activity $i", Color(LightActivityColours[i]), h)
    } + ChartSlice("Unlogged", UnloggedColour, 140)

    DonutChart(
        slices = slices,
        ringFraction = 0.32f,
        selectedLabel = "Activity 1",
        modifier = Modifier.width(240.dp)
    )
}
