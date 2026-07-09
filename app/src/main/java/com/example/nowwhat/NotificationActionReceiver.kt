package com.example.nowwhat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.runBlocking

@SuppressLint("MissingPermission")
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getLongExtra("activity_id", -1L)
        val timestamp = intent.getLongExtra("timestamp", -1L)
        if (activityId == -1L || timestamp == -1L) return

        val dao = AppDatabase.getDatabase(context).hourEntryDao()

        runBlocking {
            val existing = dao.getByTimestamp(timestamp)
            if (existing != null) {
                dao.updateActual(timestamp, activityId)
            } else {
                dao.insert(
                    HourEntry(timestamp = timestamp, plannedActivityId = null, actualActivityId = activityId)
                )
            }
        }

        // Dismiss the notification
        NotificationManagerCompat.from(context).cancel(1)
    }
}