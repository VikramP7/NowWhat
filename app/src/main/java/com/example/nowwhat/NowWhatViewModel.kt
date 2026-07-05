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

    private val settingsStore = SettingsStore(application)

    private val _selectedTimestamp = MutableStateFlow(defaultTimestamp())
    private val _selectedIsFuture = MutableStateFlow(false)
    val selectedTimestamp: StateFlow<Long> = _selectedTimestamp
    val selectedIsFuture: StateFlow<Boolean> = _selectedIsFuture

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

    /* ---------------------------- APP VALs ----------------------------*/
    val days = combine(entriesFlow, activitiesFlow, settingsStore.dayStartHour) { entries, activities, startHour ->
        transformIntoDays(entries, activities, startHour)
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

    /* ---------------------------- HOUR LOGGING HELPER FUNCTIONS ----------------------------*/
    private fun transformIntoDays(entries: List<HourEntry>, activities: List<Activity>, dayStartHour: Int): List<Day> {
        //if (entries.isEmpty()) return emptyList()

        // Build a lookup map by activityId to Activity object
        val activityMap: Map<Long, Activity> = activities.associateBy { it.id }

        // Group entries by date
        // Convert each timestamp to a LocalDate
        val grouped: Map<LocalDate, List<HourEntry>> = entries.groupBy { entry ->
            logicalDateOf(entry.timestamp, dayStartHour)
        }

        // synthesizing an empty day if no entries have been made for that day yet
        val today = logicalDateOf(System.currentTimeMillis(), dayStartHour)
        val groupedWithToday =
            if (grouped.contains(today)) grouped
            else grouped+(today to emptyList())

        // For each date, build a Day object
        val formatter = DateTimeFormatter.ofPattern("EEEE · MMM d")

        return groupedWithToday.entries.sortedByDescending { it.key }.map { (date, dayEntries) ->

            // Build 24-hour slots, one per hour, all starting null
            val hourSlots = arrayOfNulls<HourSlot>(24)

            // Place each entry into the correct hour slot
            dayEntries.forEach { entry ->
                val hour = hourOfDay(entry.timestamp)

                hourSlots[hour] = HourSlot(
                    planned = entry.plannedActivityId?.let { activityMap[it] },
                    actual = entry.actualActivityId?.let { activityMap[it] }
                )
            }

            // Slice into 4 bands
            // Morning(6-11), Day(12-17), Evening(18-23), Night(0-5)
            val rows = (0..3).map { band ->
                (0..5).map { offset ->
                    hourSlots[(dayStartHour + (band * 6) + offset) % 24]
                }
            }

            Day(date = date.format(formatter), hourRows = rows, localDate = date)
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
            _selectedTimestamp.value = timestampOf(day.localDate, hourIndex, dayStartHour.value)

            _selectedIsFuture.value = _selectedTimestamp.value > defaultTimestamp()
        }
    }

    fun clearSelection() {
        _selectedTimestamp.value = defaultTimestamp()
        _selectedIsFuture.value = false
    }

    /* ---------------------------- HOUR LOGGING FUNCTIONS ----------------------------*/
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
            if (timestamp <= defaultTimestamp()){
                if (existing != null) {
                    hourEntryDao.updateActual(timestamp, activityId)
                } else {
                    hourEntryDao.insert(
                        HourEntry(timestamp = timestamp, plannedActivityId = null, actualActivityId = activityId)
                    )
                }
            }
            //clearSelection()
        }
    }


    /* ---------------------------- ACTIVITY FUNCTIONS ----------------------------*/
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

    /* ---------------------------- SETTINGS VALs ----------------------------*/
    val is24Hour = settingsStore.is24Hour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val dayStartHour = settingsStore.dayStartHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 6
    )

    /* ---------------------------- SETTINGS FUNCTIONS ----------------------------*/
    fun setIs24Hour(value: Boolean) {
        viewModelScope.launch {
            settingsStore.setIs24Hour(value)
        }
    }

    fun setDayStartHour(value: Int) {
        viewModelScope.launch {
            settingsStore.setDayStartHour(value)
        }
    }

}