package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.OffWhite
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun ActivitiesSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val activities by viewModel.activities.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = OffWhite,
        topBar = {
            TopBarSettings(
                onClick = { onNavigate(AppScreenState.SETTINGS) },
                path = " > Edit Activities"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activities) { activity ->

                var showColorPicker by remember { mutableStateOf(false) }
                var nameInput by remember { mutableStateOf(activity.name) }

                SettingRow(
                    label = activity.name,
                    leading = {
                        val swatchShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(elevation = 6.dp, shape = swatchShape)
                                .clip(swatchShape)
                                .background(Color(activity.colour))
                                .clickable { showColorPicker = !showColorPicker }
                        )
                    },
                    content = {
                        BasicTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextColour),
                            cursorBrush = SolidColor(TextColour),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        viewModel.updateActivity(nameInput, activity.colour, activity.id)
                                    }
                                },
                            decorationBox = { innerTextField ->
                                if (nameInput.isEmpty()) {
                                    Text(
                                        "name...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextColour.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Delete",
                        tint = TextColour,
                        modifier = Modifier.clickable { viewModel.removeActivity(activity) }
                    )
                }

                if(showColorPicker){
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LightActivityColours.forEach { colour ->
                            val shape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(
                                        elevation = 5.dp,
                                        shape = shape
                                    )
                                    .clip(shape)
                                    .background(Color(colour))
                                    .clickable {
                                        viewModel.updateActivity(activity.name, colour, activity.id)
                                        showColorPicker = false
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                Button(onClick = {viewModel.addActivity(
                    Activity(
                        name = "",
                        colour = LightActivityColours[0]
                    )
                )}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Add"
                    )
                }
            }
        }
    }
}
