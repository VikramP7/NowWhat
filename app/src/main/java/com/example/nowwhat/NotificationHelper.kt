package com.example.nowwhat

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate

object NotificationHelper {

    const val HOURLY_CHANNEL_ID = "nowwhat_hourly"
    const val HOURLY_NOTIFICATION_ID = 1
    private const val HOURLY_CHANNEL_NAME = "Hourly Reminder"
    private const val HOURLY_CHANNEL_DESC = "Reminds you to log the past hour and plan the next"

    const val SYNOPSIS_CHANNEL_ID = "nowwhat_synopsis"
    const val SYNOPSIS_NOTIFICATION_ID = 2
    private const val SYNOPSIS_CHANNEL_NAME = "Note Reminder"
    private const val SYNOPSIS_CHANNEL_DESC = "Reminds you to log the daily note"
    const val KEY_SYNOPSIS_REPLY = "synopsis_reply"   // names the text field in the results bundle
    const val EXTRA_EPOCH_DAY = "epoch_day"

    fun isInDndWindow(dndStart: Int, dndEnd: Int): Boolean{
        return withinHourSpan(System.currentTimeMillis(), dndStart, dndEnd)
    }

    fun createChannels(context: Context) {
        val hourlyChannel = NotificationChannel(
            HOURLY_CHANNEL_ID,
            HOURLY_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = HOURLY_CHANNEL_DESC
        }

        val synopsisChannel = NotificationChannel(
            SYNOPSIS_CHANNEL_ID,
            SYNOPSIS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = SYNOPSIS_CHANNEL_DESC
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(hourlyChannel)
        manager.createNotificationChannel(synopsisChannel)
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
        val builder = NotificationCompat.Builder(context, HOURLY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
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
        NotificationManagerCompat.from(context).notify(HOURLY_NOTIFICATION_ID, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun postSynopsisNotification(
        context: Context,
        epochDay: Long
    ){
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context,
            200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationMessage = "${LocalDate.ofEpochDay(epochDay).format(DateFormatter)} | " +
                "What happened today?"
        val builder = NotificationCompat.Builder(context, SYNOPSIS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("NowWhat")
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // The text field itself. The key is how the receiver finds what was typed.
        val remoteInput = RemoteInput.Builder(KEY_SYNOPSIS_REPLY)
            .setLabel("How was today?")
            .build()

        val replyIntent = Intent(context, NoteReplyReceiver::class.java).apply {
            putExtra(EXTRA_EPOCH_DAY, epochDay)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            300, // distinct from the hourly actions (100+) and tap-to-open (200)
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE   // MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(0, "Write synopsis", replyPendingIntent)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)  // no smart-reply suggestions for a journal entry
            .build()

        builder.addAction(replyAction)

        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(SYNOPSIS_NOTIFICATION_ID, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun postSynopsisSavedNotification(context: Context, epochDay: Long) {
        val notification = NotificationCompat.Builder(context, SYNOPSIS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("NowWhat")
            .setContentText("Synopsis saved for ${LocalDate.ofEpochDay(epochDay).format(DateFormatter)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)   // an acknowledgement, not a new prompt
            .setAutoCancel(true)
            .setTimeoutAfter(3_000L)   // system clears it after 4s so you don't have to swipe
            .build()

        NotificationManagerCompat.from(context).notify(SYNOPSIS_NOTIFICATION_ID, notification)
    }

    fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // Safety check — on API 31+ we need the exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return   // silently skip handle this properly later
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