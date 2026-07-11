package com.example.nowwhat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.nowwhat.ui.theme.LightActivityColours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter


val DEFAULT_ACTIVITY_NAMES: List<String> = listOf("Work", "Sleep", "Gym", "Social", "Dating")

class NowWhatViewModel(application: Application) : AndroidViewModel(application) {

    /* ---------------------------- FLOWS AND DBs ----------------------------*/
    private val db = AppDatabase.getDatabase(application)

    private val hourEntryDao = db.hourEntryDao()
    private val entriesFlow: Flow<List<HourEntry>> = hourEntryDao.getAll()

    private val activityDao = db.activityDao()
    private val activitiesFlow: Flow<List<Activity>> = activityDao.getAll()

    private val scheduleDao = db.scheduleDao()
    private val scheduleFlow: Flow<List<ScheduleEntry>> = scheduleDao.getAll()

    private val settingsStore = SettingsStore(application)

    private val _selectedTimestamp = MutableStateFlow(defaultTimestamp())
    private val _selectedIsFuture = MutableStateFlow(true)
    val selectedTimestamp: StateFlow<Long> = _selectedTimestamp
    val selectedIsFuture: StateFlow<Boolean> = _selectedIsFuture


    /* ---------------------------- INIT ----------------------------*/
    init {
        seedSafeDefaultActivities()

        viewModelScope.launch {
            seedToday()
        }

        viewModelScope.launch {
            settingsStore.dayStartHour
                .drop(1)              // skip the initial emission
                .collect { seedToday() }
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
            // eg. Morning(6-11), Day(12-17), Evening(18-23), Night(0-5)
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
        return truncateToHour(System.currentTimeMillis())
    }

    fun selectHour(dayIndex: Int, hourIndex: Int) {
        val daysList = days.value
        if (dayIndex in daysList.indices) {
            val day = daysList[dayIndex]
            _selectedTimestamp.value = timestampOf(day.localDate, hourIndex, dayStartHour.value)
            _selectedIsFuture.value = _selectedTimestamp.value >= defaultTimestamp()
        }
    }

    fun clearSelection() {
        _selectedTimestamp.value = defaultTimestamp()
        _selectedIsFuture.value = true
    }

    private suspend fun seedToday(){
        val startHour = settingsStore.dayStartHour.first()
        val today = logicalDateOf(System.currentTimeMillis(), startHour)
        seedDayFromSchedule(
            logicalDate = today,
            startHour = startHour,
            hourEntryDao = hourEntryDao,
            scheduleDao = scheduleDao,
            settingsStore = settingsStore)
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
            if (!_selectedIsFuture.value){
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

    fun clearAllHourEntries(){
        viewModelScope.launch {
            hourEntryDao.deleteAll()
            settingsStore.setLastSeededDay(-1L)
            seedToday()
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

    fun resetActivities(){
        viewModelScope.launch {
            val curActivities = activitiesFlow.first()
            DEFAULT_ACTIVITY_NAMES.forEachIndexed  { index, activityName ->
                val activity = curActivities.getOrNull(index)
                if (activity == null){
                    activityDao.insert(Activity(
                        name = activityName,
                        colour = LightActivityColours[index])
                    )
                }else{
                    activityDao.updateActivity(
                        name = activityName,
                        colour = LightActivityColours[index],
                        activityId = activity.id
                    )
                }
            }
            curActivities.drop(DEFAULT_ACTIVITY_NAMES.size).forEach { activityDao.delete(it) }
        }
    }

    private fun seedSafeDefaultActivities(){
        viewModelScope.launch {
            val existing = activityDao.getAll().first()
            if (existing.isEmpty()) {
                DEFAULT_ACTIVITY_NAMES.forEachIndexed  { index, activityName ->
                    activityDao.insert(Activity(activityName, LightActivityColours[index]))
                }
            }
        }
    }


    /* ---------------------------- DEFAULT SCHEDULE VALs ----------------------------*/
    private val _selectedScheduleHour = MutableStateFlow(hourOfDay(System.currentTimeMillis()))
    val selectedScheduleHour: StateFlow<Int> = _selectedScheduleHour

    private val _selectedScheduleDay = MutableStateFlow(dayOfWeek(System.currentTimeMillis()))
    val selectedScheduleDay: StateFlow<Int> = _selectedScheduleDay

    val scheduleRows: StateFlow<List<List<HourSlot?>>> = combine(
        scheduleFlow, activitiesFlow, _selectedScheduleDay, settingsStore.dayStartHour
    ) { schedule, activities, selectedDay, startHour ->
        transformScheduleIntoRows(schedule, activities, selectedDay, startHour)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())


    /* ---------------------------- DEFAULT SCHEDULE HELPER FUNCTIONS ----------------------------*/

    fun setSelectedScheduleDay(day: Int) { _selectedScheduleDay.value = day }
    fun setSelectedScheduleHour(hour: Int) { _selectedScheduleHour.value = hour }

    private fun transformScheduleIntoRows(schedule: List<ScheduleEntry>, activities: List<Activity>,selectedDay: Int, dayStartHour: Int): List<List<HourSlot?>> {
        // Filter out all other schedule entries except for the one that matches the selected day
        val selectedDaySchedule = schedule.filter { partOfLogicalDay(it.dayOfWeek, it.hourOfDay, selectedDay, dayStartHour)}

        // Build a lookup map by activityId to Activity object
        val activityMap: Map<Long, Activity> = activities.associateBy { it.id }

        // Build 24-hour slots, one per hour, all starting null
        val hourSlots = arrayOfNulls<HourSlot>(24)
        selectedDaySchedule.forEach { entry ->
            hourSlots[entry.hourOfDay] = HourSlot(
                planned = entry.plannedActivityId?.let { activityMap[it] },
                actual = null
            )
        }

        return (0..3).map { band ->
            (0..5).map { offset ->
                hourSlots[(dayStartHour + (band * 6) + offset) % 24]
            }
        }
    }

    /* ---------------------------- DEFAULT SCHEDULE DB FUNCTIONS ----------------------------*/
    fun setScheduleSlot(scheduleEntry: ScheduleEntry){
        viewModelScope.launch {
            scheduleDao.upsertSlot(scheduleEntry)
        }
    }

    fun clearScheduleSlot(dayOfWeek: Int, hourOfDay: Int){
        viewModelScope.launch {
            scheduleDao.clearSlot(dayOfWeek, hourOfDay)
        }
    }

    fun clearAllScheduleSlots(){
        viewModelScope.launch {
            scheduleDao.deleteAll()
        }
    }

    /* ---------------------------- DEFAULT SCHEDULE LOGGING FUNCTIONS ----------------------------*/
    fun logScheduledActivity(activityId: Long) {
        val hour = _selectedScheduleHour.value
        val weekday = scheduleWeekdayFor(hour, _selectedScheduleDay.value, dayStartHour.value)
        setScheduleSlot(ScheduleEntry(dayOfWeek = weekday, hourOfDay = hour, plannedActivityId = activityId))
    }

    fun logClearSelectedActivity(){
        val hour = _selectedScheduleHour.value
        val weekday = scheduleWeekdayFor(hour, _selectedScheduleDay.value, dayStartHour.value)
        clearScheduleSlot(weekday, hour)
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

    val notificationsEnabled = settingsStore.notificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val dndStartHour = settingsStore.dndStartHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 22
    )

    val dndEndHour = settingsStore.dndEndHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 7
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

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(value) }
    }

    fun setDndStartHour(value: Int) {
        viewModelScope.launch { settingsStore.setDndStartHour(value) }
    }

    fun setDndEndHour(value: Int) {
        viewModelScope.launch { settingsStore.setDndEndHour(value) }
    }

    /* ---------------------------- IMPORT/EXPORT JSON FUNCTIONS ----------------------------*/

    fun exportTo(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                val data = BackupData(
                    activities = activitiesFlow.first(),
                    entries = entriesFlow.first(),
                    schedule = scheduleFlow.first()
                )
                val json = toJson(data)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri, "wt")
                        ?.use { it.write(json.toByteArray()) }
                        ?: throw IOException("Couldn't open output stream")
                }
                true
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }

    fun importFrom(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                        ?: throw IOException("Couldn't open input stream")
                }
                val data = fromJson(text)          // throws on a malformed file
                db.withTransaction {
                    hourEntryDao.deleteAll()
                    scheduleDao.deleteAll()
                    activityDao.deleteAll()
                    activityDao.insertAll(data.activities)
                    hourEntryDao.insertAll(data.entries)
                    scheduleDao.insertAll(data.schedule)
                }
                true
            } catch (e: Exception) {
                false
            }
            onResult(success)
        }
    }

}