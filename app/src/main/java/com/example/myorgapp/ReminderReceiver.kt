package com.example.myorgapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Reminder", "ReminderReceiver.onReceive called")
        val cardId = intent.getLongExtra(NotificationHelper.EXTRA_CARD_ID, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, cardId.toInt())
        Log.d("Reminder", "ReminderReceiver: cardId=$cardId, notifId=$notificationId")
        if (cardId == -1L) {
            Log.e("Reminder", "ReminderReceiver: no cardId in intent extras")
            return
        }

        val prefs = context.getSharedPreferences("card_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<List<CardItem>>() {}.type

        val json = prefs.getString("cards_json", null)
        if (json == null) {
            Log.e("Reminder", "ReminderReceiver: cards_json is null in prefs")
            return
        }

        val allCards: List<CardItem>?
        try {
            allCards = gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("Reminder", "ReminderReceiver: gson parse failed: $e")
            return
        }
        if (allCards == null) {
            Log.e("Reminder", "ReminderReceiver: gson returned null")
            return
        }

        val card = allCards.find { it.id == cardId }
        if (card == null) {
            Log.e("Reminder", "ReminderReceiver: card $cardId not found in prefs")
            return
        }
        Log.d("Reminder", "ReminderReceiver: found card '${card.name}'")

        NotificationHelper.showReminderNotification(context, card, notificationId)
    }
}
