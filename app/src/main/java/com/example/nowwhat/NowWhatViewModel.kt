package com.example.nowwhat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nowwhat.ui.theme.LightActivityColours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NowWhatViewModel(application: Application) : AndroidViewModel(application) {

    private val hourEntryDao = AppDatabase.getDatabase(application).hourEntryDao()
    private val entriesFlow: Flow<List<HourEntry>> = hourEntryDao.getAll()

    private val activityDao = AppDatabase.getDatabase(application).activityDao()
    private val activitiesFlow: Flow<List<Activity>> = activityDao.getAll()

    private val _selectedTimestamp = MutableStateFlow(defaultTimestamp())
    val selectedTimestamp: StateFlow<Long> = _selectedTimestamp

    init {
        viewModelScope.launch {
            val existing = activityDao.getAll().first()
            if (existing.isEmpty()) {
                activityDao.insert(Activity("Work", LightActivityColours[0]))
                activityDao.insert(Activity("Sleep", LightActivityColours[1]))
                activityDao.insert(Activity("Gym", LightActivityColours[2]))
                activityDao.insert(Activity("Social", LightActivityColours[3]))
                activityDao.insert(Activity("Dating", LightActivityColours[4]))
            }
        }
    }

    private fun transformIntoDays(entries: List<HourEntry>, activities: List<Activity>): List<Day> {
        if (entries.isEmpty()) return emptyList()

        // Build a lookup map by activityId to Activity object
        val activityMap: Map<Long, Activity> = activities.associateBy { it.id }

        // Group entries by date
        // Convert each timestamp to a LocalDate
        val grouped: Map<LocalDate, List<HourEntry>> = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        // For each date, build a Day object
        val formatter = DateTimeFormatter.ofPattern("EEEE · MMM d")

        return grouped.entries.sortedByDescending { it.key }.map { (date, dayEntries) ->

            // Build 24-hour slots, one per hour, all starting null
            val hourSlots = arrayOfNulls<HourSlot>(24)

            // Place each entry into the correct hour slot
            dayEntries.forEach { entry ->
                val hour = Instant.ofEpochMilli(entry.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .hour

                hourSlots[hour] = HourSlot(
                    planned = entry.plannedActivityId?.let { activityMap[it] },
                    actual = entry.actualActivityId?.let { activityMap[it] }
                )
            }

            // Slice into 4 bands
            // Morning(6-11), Day(12-17), Evening(18-23), Night(0-5)
            val rows = listOf(
                hourSlots.slice(6..11),
                hourSlots.slice(12..17),
                hourSlots.slice(18..23),
                hourSlots.slice(0..5)
            )

            Day(date = date.format(formatter), hourRows = rows, localDate = date)
        }
    }

    val days = combine(entriesFlow, activitiesFlow) { entries, activities ->
        transformIntoDays(entries, activities)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activities = activitiesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun logPlannedActivity(activityId: Long) {
        viewModelScope.launch {
            val timestamp = _selectedTimestamp.value
            val existing = hourEntryDao.getByTimestamp(timestamp)
            if (existing != null){
                hourEntryDao.updatePlanned(timestamp, activityId)
            }else{
                hourEntryDao.insert(
                    HourEntry(timestamp = timestamp, plannedActivityId = activityId, actualActivityId = null)
                )
            }
            //clearSelection()
        }
    }

    fun logActualActivity(activityId: Long) {
        viewModelScope.launch {
            val timestamp = _selectedTimestamp.value
            val existing = hourEntryDao.getByTimestamp(timestamp)
            if (existing != null) {
                hourEntryDao.updateActual(timestamp, activityId)
            } else {
                hourEntryDao.insert(
                    HourEntry(timestamp = timestamp, plannedActivityId = null, actualActivityId = activityId)
                )
            }
            //clearSelection()
        }
    }

    private fun defaultTimestamp(): Long {
        // Truncate to current hour
        return (System.currentTimeMillis() / 3_600_000) * 3_600_000
    }

    fun selectHour(dayIndex: Int, hourIndex: Int) {
        val daysList = days.value
        if (dayIndex in daysList.indices) {
            val day = daysList[dayIndex]
            _selectedTimestamp.value = day.localDate
                .atTime(hourIndex, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    fun clearSelection() {
        _selectedTimestamp.value = defaultTimestamp()
    }

    fun addActivity(activity: Activity){
        viewModelScope.launch {
            activityDao.insert(activity)
        }
    }

    fun updateActivity(name: String, colour: Int, activityId: Long){
        viewModelScope.launch {
            activityDao.updateActivity(name, colour, activityId)
        }
    }

    fun removeActivity(activity: Activity){
        viewModelScope.launch {
            activityDao.delete(activity)
        }
    }
}