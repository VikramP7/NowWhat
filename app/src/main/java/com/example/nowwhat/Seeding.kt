package com.example.nowwhat

import kotlinx.coroutines.flow.first
import java.time.LocalDate

suspend fun seedDayFromSchedule(
    logicalDate: LocalDate,
    startHour: Int,
    hourEntryDao: HourEntryDao,
    scheduleDao: ScheduleDao,
    settingsStore: SettingsStore
) {
    // 1. Guard: already seeded? (DataStore marker — see below)
    if (settingsStore.lastSeededDay.first() == logicalDate.toEpochDay()) return
    val schedule = scheduleDao.getAll().first()          // one-shot read of current schedule

    // 2. For each of the 24 hours in this logical day...
    for (offset in 0 until 24) {
        val hour = (startHour + offset) % 24
        // which weekday does this clock hour belong to?
        val weekday = scheduleWeekdayFor(hour, logicalDate.dayOfWeek.value, startHour)
        val planned = schedule.firstOrNull { it.dayOfWeek == weekday && it.hourOfDay == hour }
            ?.plannedActivityId ?: continue      // no template for this slot → skip

        // 3. Fill-only: skip if this timestamp already has an entry
        val timestamp = timestampOf(logicalDate, hour, startHour)
        val existing = hourEntryDao.getByTimestamp(timestamp)
        when {
            existing == null ->
                hourEntryDao.insert(HourEntry(timestamp, plannedActivityId = planned, actualActivityId = null))
            existing.plannedActivityId == null ->
                hourEntryDao.updatePlanned(timestamp, planned)
            else -> { /* planned already set manually — leave it */ }
        }
    }

    // 4. Mark seeded
    settingsStore.setLastSeededDay(logicalDate.toEpochDay())
}