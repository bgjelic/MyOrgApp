package com.example.myorgapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TodayCardsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences("card_pref", Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<List<CardItem>>() {}.type

        val json = prefs.getString("cards_json", null) ?: return
        val allCards: List<CardItem>? = try {
            gson.fromJson(json, type)
        } catch (_: Exception) { return }
        if (allCards == null) return

        val todayCards = getTodayCards(allCards)
        if (todayCards.isEmpty()) return

        val currentIndex = prefs.getInt("notif_current_card_index", 0)

        when (action) {
            "PREV" -> {
                val newIndex = if (currentIndex > 0) currentIndex - 1 else todayCards.size - 1
                prefs.edit().putInt("notif_current_card_index", newIndex).apply()
                NotificationHelper.showTodayCardsNotification(context, todayCards, newIndex)
            }
            "NEXT" -> {
                val newIndex = if (currentIndex < todayCards.size - 1) currentIndex + 1 else 0
                prefs.edit().putInt("notif_current_card_index", newIndex).apply()
                NotificationHelper.showTodayCardsNotification(context, todayCards, newIndex)
            }
            "TOGGLE" -> {
                val card = todayCards.getOrNull(currentIndex) ?: return
                val toggled = if (card.finished) {
                    card.copy(finished = false, dateCompleted = null)
                } else {
                    card.copy(finished = true, dateCompleted = DateHelper.todayDate())
                }

                val updatedAllCards = allCards.map { if (it.id == card.id) toggled else it }
                prefs.edit().putString("cards_json", gson.toJson(updatedAllCards)).apply()

                val completedType = object : TypeToken<List<CardItem>>() {}.type
                val completedJson = prefs.getString("completed_cards_json", null)
                val completedCards: MutableList<CardItem> = if (completedJson != null) {
                    try {
                        (gson.fromJson(completedJson, completedType) as? List<CardItem>)?.toMutableList() ?: mutableListOf()
                    } catch (_: Exception) { mutableListOf() }
                } else { mutableListOf() }

                if (toggled.finished) {
                    completedCards.add(toggled)
                } else {
                    completedCards.removeAll { it.id == toggled.id }
                }
                prefs.edit().putString("completed_cards_json", gson.toJson(completedCards)).apply()

                val updatedToday = getTodayCards(updatedAllCards)
                if (updatedToday.isEmpty()) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.cancel(NotificationHelper.TODAY_CARDS_NOTIFICATION_ID)
                    return
                }
                val newIndex = currentIndex.coerceAtMost(updatedToday.size - 1)
                prefs.edit().putInt("notif_current_card_index", newIndex).apply()
                NotificationHelper.showTodayCardsNotification(context, updatedToday, newIndex)
            }
        }
    }

    private fun getTodayCards(cards: List<CardItem>): List<CardItem> {
        val today = DateHelper.todayDate()
        return cards.filter { card ->
            !card.finished && card.taskSetTimeStart?.startsWith(today) == true
        }.sortedBy { it.taskSetTimeStart }
    }
}
