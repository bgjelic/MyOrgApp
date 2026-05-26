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
    const val TODAY_CARDS_NOTIFICATION_ID = 10000
    private const val PREV_REQUEST_CODE = 2001
    private const val NEXT_REQUEST_CODE = 2002
    private const val TOGGLE_REQUEST_CODE = 2003

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

    fun showTodayCardsNotification(context: Context, cards: List<CardItem>, currentIndex: Int) {
        if (cards.isEmpty()) return
        val safeIndex = currentIndex.coerceIn(0, cards.size - 1)
        val card = cards[safeIndex]

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CARD_ID, card.id)
            putExtra(EXTRA_CARD_NAME, card.name)
            putExtra(EXTRA_CARD_DESC, card.description)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, card.id.toInt(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(context, TodayCardsReceiver::class.java).apply { action = "PREV" }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context, PREV_REQUEST_CODE, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(context, TodayCardsReceiver::class.java).apply { action = "NEXT" }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, NEXT_REQUEST_CODE, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(context, TodayCardsReceiver::class.java).apply { action = "TOGGLE" }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context, TOGGLE_REQUEST_CODE, toggleIntent,
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
            .setContentTitle("Today's Cards (${safeIndex + 1} of ${cards.size})")
            .setContentText(card.name)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_notification_bell, "\u25C0 Prev", prevPendingIntent)
            .addAction(R.drawable.ic_notification_bell, if (card.finished) "Undo" else "Done", togglePendingIntent)
            .addAction(R.drawable.ic_notification_bell, "Next \u25B6", nextPendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(TODAY_CARDS_NOTIFICATION_ID, notification)
    }

    fun showReminderNotification(context: Context, card: CardItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_CARD_ID, card.id)
            putExtra(EXTRA_CARD_NAME, card.name)
            putExtra(EXTRA_CARD_DESC, card.description)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, card.id.toInt(), intent,
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
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(card.id.toInt(), notification)
    }
}
