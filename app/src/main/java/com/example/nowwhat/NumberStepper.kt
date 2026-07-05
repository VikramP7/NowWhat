package com.example.nowwhat

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 23,
    format: (Int) -> String = { it.toString() }
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange((value - 1).coerceIn(min, max)) }) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = "Decrease",
                tint = TextColour
            )
        }
        Text(
            text = format(value),
            style = MaterialTheme.typography.bodyLarge,
            color = TextColour
        )
        IconButton(onClick = { onValueChange((value + 1).coerceIn(min, max)) }) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Increase",
                tint = TextColour
            )
        }
    }
}

@Preview
@Composable
fun NumberStepperPreview() {
    NumberStepper(value = 6, onValueChange = {}, format = { "%02d:00".format(it) })
}