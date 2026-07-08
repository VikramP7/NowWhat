package com.example.nowwhat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.nowwhat.NotificationHelper.scheduleNextAlarm

@SuppressLint("MissingPermission")
class  BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        scheduleNextAlarm(context)
    }
}