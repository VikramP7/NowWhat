package com.example.nowwhat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nowwhat.NotificationHelper.isInDndWindow
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@SuppressLint("MissingPermission")
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        scheduleNextAlarm(context)

        val store = SettingsStore(context)
        val enabled = runBlocking { store.notificationsEnabled.first() }
        val dndStart = runBlocking { store.dndStartHour.first() }
        val dndEnd = runBlocking { store.dndEndHour.first() }

        if (!enabled) return
        if (isInDndWindow(dndStart, dndEnd)) return

        val hourEntryDao = AppDatabase.getDatabase(context).hourEntryDao()
        val activityDao = AppDatabase.getDatabase(context).activityDao()
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
        NotificationHelper.postHourlyNotification(context, lastHour, suggestions.toList())
    }
}