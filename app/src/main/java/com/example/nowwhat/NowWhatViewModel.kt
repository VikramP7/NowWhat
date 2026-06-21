package com.example.nowwhat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NowWhatViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).hourEntryDao()
    public val entries: Flow<List<HourEntry>> = dao.getAll()

    val activities: List<Activity> = listOf(
    Activity("Work", 0xFF4CAF50.toInt(), id = 1),
    Activity("Sleep", 0xFF3F51B5.toInt(), id = 2),
    Activity("Gym", 0xFFFF9800.toInt(), id = 3),
    Activity("Social", 0xFFE91E63.toInt(), id = 4),
    Activity("Dating", 0xFFF48FB1.toInt(), id = 5)
    )

    private val sleep = activities[1]
    private val work = activities[0]
    private val gym = activities[2]
    private val social = activities[3]

    val days: List<Day> = listOf(
        Day(date = "Today · Thu Jun 19", hourRows = listOf(
            listOf(HourSlot(sleep, sleep), HourSlot(gym, gym), HourSlot(work, work),
                HourSlot(work, work), HourSlot(work, work), HourSlot(work, social)),
            listOf(HourSlot(work, work), HourSlot(work, work), HourSlot(work, work),
                HourSlot(work, gym), HourSlot(gym, gym), HourSlot(social, social)),
            listOf(HourSlot(social, social), HourSlot(social, social), HourSlot(planned = social),
                HourSlot(), HourSlot(), HourSlot()),
            listOf(HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep),
                HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep))
        )),
        Day(date = "Yesterday · Wed Jun 18", hourRows = listOf(
            listOf(HourSlot(sleep, sleep), HourSlot(sleep, gym), HourSlot(gym, gym),
                HourSlot(work, work), HourSlot(work, work), HourSlot(work, work)),
            listOf(HourSlot(work, work), HourSlot(work, social), HourSlot(social, social),
                HourSlot(work, work), HourSlot(work, work), HourSlot(gym, gym)),
            listOf(HourSlot(social, social), HourSlot(actual = social), HourSlot(planned = gym),
                HourSlot(social, social), HourSlot(sleep, sleep), HourSlot(sleep, sleep)),
            listOf(HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep),
                HourSlot(sleep, sleep), HourSlot(sleep, sleep), HourSlot(sleep, sleep))
        ))
    )

    public fun addEntry(didWhat: String, nowWhat: String) {
        viewModelScope.launch {
            val entry = HourEntry(
                timestamp = System.currentTimeMillis(),
                didWhat=didWhat,
                nowWhat=nowWhat
            )
            dao.insert(entry)
        }
    }
}