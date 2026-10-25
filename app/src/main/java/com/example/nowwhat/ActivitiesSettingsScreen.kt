package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nowwhat.ui.theme.BackgroundColour
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.TextColour
import com.example.nowwhat.ui.theme.inkOn

@Composable
fun ActivitiesSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: NowWhatViewModel = viewModel(),
    onNavigate: (nextScreenState: AppScreenState) -> Unit
) {

    val activities by viewModel.activities.collectAsState()
    val showInitials by viewModel.showInitials.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundColour,
        topBar = {
            TopBarSettings(
                onClick = { onNavigate(AppScreenState.SETTINGS) },
                path = " > Edit Activities"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            SettingRow(
                label = "Show Initials",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Switch(
                    checked = showInitials,
                    onCheckedChange = { viewModel.setShowInitials(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundColour,
                        checkedTrackColor = TextColour,
                        uncheckedThumbColor = TextColour,
                        uncheckedTrackColor = BackgroundColour,
                        uncheckedBorderColor = TextColour
                    )
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                // top = 8 leaves room for the first card's shadow, as on Notes and Statistics
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activities, key = { it.id }) { activity ->

                    var showColorPicker by remember { mutableStateOf(false) }
                    var nameInput by remember { mutableStateOf(activity.name) }

                    SettingRow(
                        label = activity.name,
                        leading = {
                            val swatchShape = RoundedCornerShape(8.dp)
                            val fill = Color(activity.colour)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(elevation = 6.dp, shape = swatchShape)
                                    .clip(swatchShape)
                                    .background(fill)
                                    .clickable { showColorPicker = !showColorPicker },
                                contentAlignment = Alignment.Center
                            ) {
                                if (showInitials) InitialLabel(name = nameInput, ink = inkOn(fill))
                            }
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

                    if (showColorPicker) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            maxItemsInEachRow = 8,                              // 16 colours -> two even rows
                            horizontalArrangement = Arrangement.SpaceBetween,   // spread each row edge to edge
                            verticalArrangement = Arrangement.spacedBy(12.dp)   // leaves room for the 5dp shadows
                        ) {
                            LightActivityColours.forEach { colour ->
                                val shape = RoundedCornerShape(8.dp)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(elevation = 5.dp, shape = shape)
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
}
