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

        NotificationHelper.postHourlyNotification(context)
    }
}