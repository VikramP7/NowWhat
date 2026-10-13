package com.example.nowwhat

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun HeadlineStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.headlineSmall
) {
    StatsCard(modifier = modifier) {
        Text(
            text = value,
            style = valueStyle,
            color = TextColour,
            maxLines = 1,
            // Shrinks the text only when it won't fit on one line: it starts at the style's own size
            // and steps down, never below 12sp. "100/120" fits instead of clipping to "100/10".
            autoSize = TextAutoSize.StepBased(
                minFontSize = 12.sp,
                maxFontSize = valueStyle.fontSize
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextColour,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
private fun HeadlineStatPreview() {
    HeadlineStat(value = "65%", label = "coverage")
}