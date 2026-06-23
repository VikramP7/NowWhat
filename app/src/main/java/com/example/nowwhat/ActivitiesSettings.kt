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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

val ACTIVITYCOLOURS: List<Int> = listOf(
    0xFF4CAF50.toInt(),
    0xFF3F51B5.toInt(),
    0xFFFF9800.toInt(),
    0xFFE91E63.toInt(),
    0xFFF48FB1.toInt(),
)

@Composable
fun ActivitiesSettings(
    modifier: Modifier,
    activities: List<Activity>,
    onAddActivity: (activity: Activity) -> Unit,
    onUpdateActivity: (name: String, colour: Int, activityId: Long) -> Unit,
    onRemoveActivity: (activity: Activity) -> Unit
){

    Text(text = "Edit Activities:")
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

                Box(modifier = Modifier.align(Alignment.CenterVertically)
                    .size(48.dp)
                    .background(Color(activity.colour))
                    .padding(2.dp)
                    .clickable{
                        showColorPicker = !showColorPicker
                    }
                )

                OutlinedTextField(
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
                LazyRow(modifier= Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceAround) {
                    itemsIndexed(ACTIVITYCOLOURS, key = {index, colour -> colour}) { index, colour ->
                        Box(modifier = Modifier
                            .size(42.dp)
                            .background(Color(colour))
                            .padding(2.dp)
                            .clickable{
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
                    colour = 0xFF4CAF50.toInt()
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