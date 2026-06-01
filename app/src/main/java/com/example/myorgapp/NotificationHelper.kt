package com.example.myorgapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "card_reminders"
    const val EXTRA_CARD_ID = "card_id"
    const val EXTRA_CARD_NAME = "card_name"
    const val EXTRA_CARD_DESC = "card_desc"
    const val EXTRA_NOTIFICATION_ID = "notif_id"
    const val ACTION_DONE_SILENT = "DONE_SILENT"
    private const val DONE_REQUEST_BASE = 10000

    fun createReminderNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Card Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for scheduled cards"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun showReminderNotification(context: Context, card: CardItem, notificationId: Int = card.id.toInt()) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CARD_ID, card.id)
            putExtra(EXTRA_CARD_NAME, card.name)
            putExtra(EXTRA_CARD_DESC, card.description)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DONE_SILENT
            putExtra(EXTRA_CARD_ID, card.id)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val doneRequestCode = DONE_REQUEST_BASE + (notificationId % 100000)
        val donePendingIntent = PendingIntent.getBroadcast(
            context, doneRequestCode, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = buildString {
            append(card.name)
            if (!card.description.isNullOrBlank()) {
                append("\n").append(card.description)
            }
            card.taskSetTimeStart?.let { t ->
                append("\n").append(t.replace("T", " "))
            }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(card.name)
            .setContentText(card.description ?: "Card reminder")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_notification_bell, "Done", donePendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }
}
