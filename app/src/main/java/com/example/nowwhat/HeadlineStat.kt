package com.example.nowwhat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun HeadlineStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    StatsCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = TextColour,
            maxLines = 1,
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