package com.example.nowwhat

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.nowwhat.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onClick: () -> Unit, // callback
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("NowWhat") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OffWhite,
            titleContentColor = TextColour,
            actionIconContentColor = TextColour
        ),
        actions = {
            IconButton(onClick = { onClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings"
                )
            }
        }
    )
}

@Preview
@Composable
fun TopBarPreview() {
    TopBar(onClick = {})
}