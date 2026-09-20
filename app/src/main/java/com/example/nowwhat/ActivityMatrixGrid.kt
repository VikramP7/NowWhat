package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.TextColour
import kotlin.math.roundToInt

@Composable
fun ActivityMatrixGrid(
    matrix: ActivityMatrix,
    normalised: Boolean,
    modifier: Modifier = Modifier
) {
    val size = matrix.axis.size
    val fractions = matrix.rowFractions()
    val maxCount = matrix.counts.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 0

    val ink = TextColour
    val paper = BackgroundColour
    val cellShape = RoundedCornerShape(4.dp)
    val gap = Arrangement.spacedBy(2.dp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = gap) {
        // Header row: an empty corner, then one swatch per column
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = gap) {
            Spacer(Modifier.weight(1f))
            matrix.axis.forEach { activity ->
                MatrixSwatch(activity, Modifier.weight(1f))
            }
        }
        matrix.axis.forEachIndexed { row, rowActivity ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = gap) {
                MatrixSwatch(rowActivity, Modifier.weight(1f))
                repeat(size) { col ->
                    val count = matrix.counts[row][col]
                    val intensity =
                        if (normalised) fractions[row][col]
                        else if (maxCount > 0) count.toFloat() / maxCount
                        else 0f
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(cellShape)
                            .background(ink.copy(alpha = 0.06f + 0.84f * intensity))
                            .then(
                                if (row == col) Modifier.border(1.5.dp, ink, cellShape)
                                else Modifier
                            )
                    ) {
                        if (count > 0) {
                            Text(
                                text = if (normalised) {
                                    val percent = fractions[row][col] * 100
                                    if (percent < 1f) "<1" else "${percent.roundToInt()}"
                                } else "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (intensity > 0.5f) paper else ink,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixSwatch(activity: Activity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(6.dp)
            .clip(CircleShape)
            .background(Color(activity.colour))
    )
}