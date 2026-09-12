package com.example.nowwhat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nowwhat.NotificationHelper.isInDndWindow
import com.example.nowwhat.NotificationHelper.postSynopsisNotification
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@SuppressLint("MissingPermission")
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        scheduleNextAlarm(context)

        val settingsStore = SettingsStore(context)
        val enabled = runBlocking { settingsStore.notificationsEnabled.first() }
        val dndStart = runBlocking { settingsStore.dndStartHour.first() }
        val dndEnd = runBlocking { settingsStore.dndEndHour.first() }
        val noteNotificationsEnabled = runBlocking { settingsStore.noteNotificationsEnabled.first() }
        val noteNotificationHour = runBlocking { settingsStore.noteNotificationHour.first() }
        val is24Hour = runBlocking { settingsStore.is24Hour.first() }

        val db = AppDatabase.getDatabase(context)
        val hourEntryDao = db.hourEntryDao()
        val activityDao = db.activityDao()
        val scheduleDao = db.scheduleDao()
        val noteDao = db.noteDao()

        val startHour = runBlocking { settingsStore.dayStartHour.first() }
        runBlocking { seedDayFromSchedule(
            logicalDate = logicalDateOf(System.currentTimeMillis(), startHour),
            startHour = startHour,
            hourEntryDao = hourEntryDao,
            scheduleDao = scheduleDao,
            settingsStore = settingsStore
        )}

        // early return if notifications are not enabled
        if (!enabled) return

        runBlocking {
            if (noteNotificationsEnabled && hourOfDay(System.currentTimeMillis()) == noteNotificationHour) {
                val epochDay = logicalDateOf(System.currentTimeMillis(), startHour).toEpochDay()
                if (noteDao.getByEpochDay(epochDay) == null) {
                    postSynopsisNotification(context, epochDay)
                }
            }
        }

        if (isInDndWindow(dndStart, dndEnd)) return

        val suggestions = mutableListOf<Activity>()
        val lastHour = truncateToHour(System.currentTimeMillis())- 3_600_000L
        val lastLastHour = lastHour - 3_600_000L
        runBlocking {
            // Primary: what did they plan for the hour that just passed?
            hourEntryDao.getByTimestamp(lastHour)?.plannedActivityId
                ?.let { activityDao.getById(it) }
                ?.let { suggestions.add(it) }

            // Secondary: what did they actually do the hour before?
            hourEntryDao.getByTimestamp(lastLastHour)?.actualActivityId
                ?.let { activityDao.getById(it) }
                ?.let { if (it !in suggestions) suggestions.add(it) }

            // Guarantee at least one suggestion
            if (suggestions.isEmpty()) {
                activityDao.getAll().first().firstOrNull()?.let { suggestions.add(it) }
            }
        }
        NotificationHelper.postHourlyNotification(context, lastHour, suggestions.toList(), is24Hour)
    }
}