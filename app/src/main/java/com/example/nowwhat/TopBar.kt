package com.example.nowwhat

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onClick: () -> Unit, // callback
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {Text("NowWhat")},
        actions = {
            IconButton(onClick = { onClick() }) {
                IconButton(onClick = { onClick() }) {
                    Text("☰")
                }
            }
        }
    )
}

@Preview
@Composable
fun TopBarPreview() {
    TopBar(onClick = {})
}