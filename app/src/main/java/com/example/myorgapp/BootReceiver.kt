package com.example.myorgapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

        val prefs = context.getSharedPreferences("card_pref", Context.MODE_PRIVATE)
        val gson = Gson()

        val cardsJson = prefs.getString("cards_json", null) ?: return
        val cards: List<CardItem> = try {
            val type = object : TypeToken<List<CardItem>>() {}.type
            gson.fromJson(cardsJson, type) ?: return
        } catch (_: Exception) { return }

        val remHour = prefs.getInt("settings_default_reminder_hour", 9)
        val remMinute = prefs.getInt("settings_default_reminder_minute", 0)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (card in cards) {
            if (card.finished) continue
            if (card.reminders.isEmpty()) continue

            for ((index, reminder) in card.reminders.withIndex()) {
                val requestCode = ((card.id % 100000) * 10 + index).toInt()

                val triggerAt: Long
                if (reminder.customTime != null) {
                    triggerAt = try {
                        val cal = DateHelper.parseDateTime(reminder.customTime)
                        if (cal.before(DateHelper.nowCal())) {
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                            if (cal.before(DateHelper.nowCal())) continue
                        }
                        cal.timeInMillis
                    } catch (_: Exception) { continue }
                } else {
                    val minutesBefore = reminder.minutesBefore ?: continue
                    val start = card.taskSetTimeStart ?: continue
                    triggerAt = try {
                        val cal = if (start.contains("T")) {
                            DateHelper.parseDateTime(start)
                        } else {
                            DateHelper.parseDate(start).apply {
                                set(Calendar.HOUR_OF_DAY, remHour)
                                set(Calendar.MINUTE, remMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        }
                        cal.add(Calendar.MINUTE, -minutesBefore)
                        if (cal.before(DateHelper.nowCal())) {
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                            if (cal.before(DateHelper.nowCal())) continue
                        }
                        cal.timeInMillis
                    } catch (_: Exception) { continue }
                }

                val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(NotificationHelper.EXTRA_CARD_ID, card.id)
                    putExtra(NotificationHelper.EXTRA_CARD_NAME, card.name)
                    putExtra(NotificationHelper.EXTRA_CARD_DESC, card.description)
                    putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
                }
                val pi = PendingIntent.getBroadcast(
                    context, requestCode, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    alarmManager.canScheduleExactAlarms()
                ) {
                    try {
                        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, null), pi)
                        continue
                    } catch (_: Exception) { }
                }
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000, pi)
            }
        }
    }
}
