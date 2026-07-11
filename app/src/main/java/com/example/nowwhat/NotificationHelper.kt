package com.example.nowwhat

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID = "nowwhat_hourly"
    private const val CHANNEL_NAME = "Hourly Reminder"
    private const val CHANNEL_DESC = "Reminds you to log the past hour and plan the next"

    fun isInDndWindow(dndStart: Int, dndEnd: Int): Boolean{
        return withinHourSpan(System.currentTimeMillis(), dndStart, dndEnd)
    }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun postHourlyNotification(
        context: Context,
        logTimestamp: Long,
        suggestions: List<Activity>,   // 0-2 activities to show as action buttons
        is24Hour: Boolean
        ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationMessage = "What did you just do from " +
                "${formatHourLabel(hourOfDay(logTimestamp), is24Hour)} - " +
                "${formatHourLabel(hourOfDay(logTimestamp)+1, is24Hour)}?"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("NowWhat")
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        suggestions.forEachIndexed { index, activity ->
            val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                putExtra("activity_id", activity.id)
                putExtra("timestamp", logTimestamp)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                100 + index,    // unique request code per action — can't reuse 0
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, activity.name, actionPendingIntent)
        }

        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(1, notification)
    }

    fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // Safety check — on API 31+ we need the exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return   // silently skip — we'll handle this properly later
        }

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,                          // request code identifies this alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set trigger time to be the top of the next hour
        val triggerTime = truncateToHour(System.currentTimeMillis()) + 3_600_000L

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,    // real wall-clock time, wakes the device
            triggerTime,
            pendingIntent
        )
    }
}