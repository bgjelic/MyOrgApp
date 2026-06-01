package com.example.myorgapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == NotificationHelper.ACTION_DONE_SILENT) {
            handleDoneSilent(context, intent)
            return
        }

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

    private fun handleDoneSilent(context: Context, intent: Intent) {
        val cardId = intent.getLongExtra(NotificationHelper.EXTRA_CARD_ID, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, cardId.toInt())
        Log.d("Reminder", "handleDoneSilent: cardId=$cardId, notifId=$notificationId")
        if (cardId == -1L) return

        val prefs = context.getSharedPreferences("card_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<List<CardItem>>() {}.type

        val json = prefs.getString("cards_json", null) ?: return
        val allCards: List<CardItem> = try {
            gson.fromJson(json, type) ?: return
        } catch (_: Exception) { return }

        val index = allCards.indexOfFirst { it.id == cardId }
        if (index == -1) return
        val card = allCards[index]
        if (card.finished) return

        val today = DateHelper.todayDate()
        val completed = card.copy(finished = true, dateCompleted = today)
        val updatedCards = allCards.toMutableList()
        updatedCards.removeAt(index)

        val completedType = object : TypeToken<List<CardItem>>() {}.type
        val completedJson = prefs.getString("completed_cards_json", null)
        val completedCards: MutableList<CardItem> = if (completedJson != null) {
            try {
                (gson.fromJson(completedJson, completedType) as? List<CardItem>)?.toMutableList() ?: mutableListOf()
            } catch (_: Exception) { mutableListOf() }
        } else { mutableListOf() }
        if (card.repeatType == RepeatType.NONE) {
            completedCards.add(completed)
        }

        prefs.edit()
            .putString("cards_json", gson.toJson(updatedCards))
            .putString("completed_cards_json", gson.toJson(completedCards))
            .apply()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
        Log.d("Reminder", "handleDoneSilent: card $cardId completed and notification $notificationId dismissed")
    }
}
