package com.example.nowwhat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.nowwhat.NotificationHelper.postSynopsisSavedNotification
import kotlinx.coroutines.runBlocking

class NoteReplyReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        // Which day this reply is for
        val epochDay = intent.getLongExtra(NotificationHelper.EXTRA_EPOCH_DAY, Long.MIN_VALUE)
        if (epochDay == Long.MIN_VALUE) return

        // The typed text does NOT arrive as a normal extra.
        // Returns null if the broadcast arrived without a reply attached.
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationHelper.KEY_SYNOPSIS_REPLY)
            ?.toString()
            ?: return

        runBlocking {
            saveTrimNote(epochDay, text, AppDatabase.getDatabase(context).noteDao())
        }

        // Once reply notification has been received send save successful notification that will
        // replace notification in channel and auto disappear
        postSynopsisSavedNotification(context, epochDay)
    }
}