package com.example.nowwhat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nowwhat.ui.theme.LightActivityColours
import com.example.nowwhat.ui.theme.TextColour

@Composable
fun ActivitiesSettings(
    modifier: Modifier,
    activities: List<Activity>,
    onAddActivity: (activity: Activity) -> Unit,
    onUpdateActivity: (name: String, colour: Int, activityId: Long) -> Unit,
    onRemoveActivity: (activity: Activity) -> Unit
){

    Text(text = "Edit Activities:", color = TextColour)
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(activities, key = {_, activity -> activity.id}) {activityIndex, activity ->
            var showColorPicker by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var nameInput by remember { mutableStateOf(activity.name) }

                val shape = RoundedCornerShape(8.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(44.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = shape
                        )
                        .clip(shape)
                        .background(Color(activity.colour))
                        .clickable{
                            showColorPicker = !showColorPicker
                        }
                )

                TextField(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .onFocusChanged {focusState ->
                            if (!focusState.isFocused) {
                                onUpdateActivity(nameInput,activity.colour,activity.id)
                            }
                        },
                    value = nameInput,
                    onValueChange = { newValue -> nameInput = newValue}, // Updates state on keystroke
                    placeholder = { Text("name..." ) }
                )

                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = { onRemoveActivity(activity) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Trash"
                    )
                }
            }

            if(showColorPicker){
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
                                    onUpdateActivity(activity.name, colour, activity.id)
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color.Gray
            )
        }
        item {
            Button(onClick = {onAddActivity(
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