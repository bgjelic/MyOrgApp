package com.example.myorgapp

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import java.util.Calendar
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SharedCardViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("card_pref", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _cards = MutableStateFlow<List<CardItem>>(emptyList())
    val cards: StateFlow<List<CardItem>> = _cards

    private val _completedCards = MutableStateFlow<List<CardItem>>(emptyList())
    val completedCards: StateFlow<List<CardItem>> = _completedCards

    private val _editing = MutableStateFlow<CardItem?>(null)
    val editing: StateFlow<CardItem?> = _editing

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    private val _yesterdayUncompleted = MutableStateFlow<List<CardItem>>(emptyList())
    val yesterdayUncompleted: StateFlow<List<CardItem>> = _yesterdayUncompleted

    private val _showYesterdayDialog = MutableStateFlow(false)
    val showYesterdayDialog: StateFlow<Boolean> = _showYesterdayDialog

    private val _toastMessage = MutableStateFlow<String?>(null)

    val toastMessage: StateFlow<String?> = _toastMessage

    private val _highlightedCardId = MutableStateFlow<Long?>(null)

    val highlightedCardId: StateFlow<Long?> = _highlightedCardId

    private var nextId = 0L

    init {
        migrateRemindersPrefs("cards_json")
        migrateRemindersPrefs("completed_cards_json")
        loadSettings()
        loadTags()
        val saved = loadFromPrefs()
        _cards.value = saved
        nextId = (saved.maxOfOrNull { it.id } ?: 0L) + 1L
        _completedCards.value = loadCompletedFromPrefs()
        runDailyScanIfNeeded()
        checkYesterdayUncompleted()
    }

    fun setEditing(card: CardItem?) {
        _editing.value = if (card == null) null
        else _cards.value.find { it.id == card.id } ?: card
    }

    fun setHighlightedCardId(id: Long?) {
        _highlightedCardId.value = id
    }

    fun addCard(
        name: String,
        description: String,
        dateCompleted: String? = null,
        finished: Boolean = false,
        taskSetTimeStart: String? = null,
        taskSetTimeEnd: String? = null,
        reminders: List<CardReminder> = emptyList(),
        reminderMinutesBefore: Int? = null,
        reminderCustomTime: String? = null,
        repeatType: RepeatType = RepeatType.NONE,
        repeatDaysOfWeek: List<Int>? = null,
        repeatEndDate: String? = null,
        repeatSkipDates: String? = null,
        checklist: List<ChecklistItem> = emptyList(),
        tagIds: List<String> = emptyList()
    ) {
        val finalReminders = if (reminders.isNotEmpty()) reminders
        else buildList {
            if (reminderMinutesBefore != null) add(CardReminder(minutesBefore = reminderMinutesBefore))
            if (reminderCustomTime != null) add(CardReminder(customTime = reminderCustomTime))
        }
        val card = CardItem(
            id = nextId++,
            name = name,
            description = description,
            dateCreated = DateHelper.todayDate(),
            dateCompleted = dateCompleted,
            finished = finished,
            taskSetTimeStart = taskSetTimeStart,
            taskSetTimeEnd = taskSetTimeEnd,
            reminders = finalReminders,
            repeatType = repeatType,
            repeatDaysOfWeek = repeatDaysOfWeek,
            repeatEndDate = repeatEndDate,
            repeatSkipDates = repeatSkipDates,
            checklist = checklist,
            tagIds = tagIds
        )
        _cards.update { it + card }
        scheduleReminder(card)
        persistAsync()
    }

    fun updateCard(updated: CardItem) {
        val old = _cards.value.find { it.id == updated.id }
        old?.let { cancelReminder(it) }
        _cards.update { list -> list.map { if (it.id == updated.id) updated else it } }
        scheduleReminder(updated)
        persistAsync()
    }

    fun deleteCard(id: Long) {
        val card = _cards.value.find { it.id == id }
        card?.let { cancelReminder(it) }
        _cards.update { list -> list.filterNot { it.id == id } }
        persistAsync()
    }

    private fun playCheckSound() {
        try {
            val customUri = prefs.getString("check_sound_uri", null)
            val uri = customUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            MediaPlayer.create(getApplication(), uri).apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (_: Exception) { }
    }

    fun toggleFinished(card: CardItem) {
        val original = _cards.value.find { it.id == card.id }
        if (original != null) {
            if (!original.finished && original.checklist.any { !it.checked }) {
                _toastMessage.value = "Complete all checklist items first"
                return
            }
            if (original.finished) {
                val newCount = if (original.repeatType != RepeatType.NONE && original.repeatCompletionCount > 0)
                    original.repeatCompletionCount - 1 else original.repeatCompletionCount
                updateCard(original.copy(
                    finished = false,
                    dateCompleted = null,
                    repeatCompletionCount = newCount
                ))
            } else {
                playCheckSound()
                val today = DateHelper.todayDate()
                if (original.repeatType != RepeatType.NONE) {
                    updateCard(original.copy(
                        finished = true,
                        dateCompleted = today,
                        repeatCompletionCount = original.repeatCompletionCount + 1
                    ))
                } else {
                    val completed = original.copy(finished = true, dateCompleted = today)
                    cancelReminder(original)
                    _cards.update { list -> list.filterNot { it.id == original.id } }
                    _completedCards.update { list -> list + completed }
                    persistAsync()
                    persistCompletedAsync()
                }
            }
            return
        }

        val completed = _completedCards.value.find { it.id == card.id } ?: return
        val restored = completed.copy(finished = false, dateCompleted = null)
        _completedCards.update { list -> list.filterNot { it.id == completed.id } }
        _cards.update { list -> list + restored }
        persistAsync()
        persistCompletedAsync()
    }

    fun getCardsForDate(dateStr: String): List<CardItem> {
        val result = mutableListOf<CardItem>()
        for (card in _cards.value) {
            val timeMatch = card.taskSetTimeStart?.let { start ->
                try {
                    val startDate = DateHelper.getDatePart(start)
                    val endDate = card.taskSetTimeEnd?.let { DateHelper.getDatePart(it) }
                    startDate == dateStr || endDate == dateStr
                } catch (_: Exception) { false }
            } ?: false
            if (timeMatch) {
                val isFinished = card.finished && card.dateCompleted == dateStr
                result.add(card.copy(finished = isFinished, dateCompleted = if (isFinished) dateStr else null))
                continue
            }
            if (card.repeatType == RepeatType.NONE) continue
            val createdDate = card.dateCreated ?: continue
            if (DateHelper.isDateMatchingRepeat(
                    date = dateStr,
                    repeatType = card.repeatType,
                    createdDate = createdDate,
                    daysOfWeek = card.repeatDaysOfWeek,
                    endDate = card.repeatEndDate,
                    skipDates = parseSkipDates(card.repeatSkipDates)
                )
            ) {
                val isFinished = card.finished && card.dateCompleted == dateStr
                val time = card.taskSetTimeStart?.let { DateHelper.getTimePart(it) }
                result.add(if (time != null) {
                    card.copy(taskSetTimeStart = "$dateStr${'T'}$time", finished = isFinished, dateCompleted = if (isFinished) dateStr else null)
                } else {
                    card.copy(taskSetTimeStart = dateStr, finished = isFinished, dateCompleted = if (isFinished) dateStr else null)
                })
            }
        }
        return result
    }

    fun getCardsForWeek(startOfWeek: String): List<CardItem> {
        val endOfWeek = DateHelper.addDays(startOfWeek, 6)
        val result = mutableListOf<CardItem>()
        for (card in _cards.value) {
            val datesInWeek = mutableSetOf<String>()

            card.taskSetTimeStart?.let { start ->
                try {
                    val sd = DateHelper.getDatePart(start)
                    if (sd >= startOfWeek && sd <= endOfWeek) {
                        datesInWeek.add(sd)
                    }
                } catch (_: Exception) {}
            }

            if (card.repeatType != RepeatType.NONE) {
                val createdDate = card.dateCreated ?: continue
                var day = startOfWeek
                while (day <= endOfWeek) {
                    if (DateHelper.isDateMatchingRepeat(
                            date = day,
                            repeatType = card.repeatType,
                            createdDate = createdDate,
                            daysOfWeek = card.repeatDaysOfWeek,
                            endDate = card.repeatEndDate,
                            skipDates = parseSkipDates(card.repeatSkipDates)
                        )
                    ) {
                        datesInWeek.add(day)
                    }
                    day = DateHelper.addDays(day, 1)
                }
            }

            val time = card.taskSetTimeStart?.let { DateHelper.getTimePart(it) }
            for (date in datesInWeek.sorted()) {
                val isFinished = card.finished && card.dateCompleted == date
                result.add(if (time != null) {
                    card.copy(taskSetTimeStart = "$date${'T'}$time", finished = isFinished, dateCompleted = if (isFinished) date else null)
                } else {
                    card.copy(taskSetTimeStart = date, finished = isFinished, dateCompleted = if (isFinished) date else null)
                })
            }
        }
        return result
    }

    fun getCardsForMonth(yearMonth: String): List<CardItem> {
        val result = mutableListOf<CardItem>()
        for (card in _cards.value) {
            val datesInMonth = mutableSetOf<String>()

            card.taskSetTimeStart?.let { start ->
                try {
                    if (DateHelper.getYearMonth(start) == yearMonth) {
                        datesInMonth.add(DateHelper.getDatePart(start))
                    }
                } catch (_: Exception) {}
            }

            if (card.repeatType != RepeatType.NONE) {
                val createdDate = card.dateCreated ?: continue
                val parts = yearMonth.split("-")
                val firstOfMonth = "${parts[0]}-${parts[1]}-01"
                val daysInMonth = DateHelper.getDaysInMonth(firstOfMonth)
                for (d in 1..daysInMonth) {
                    val day = "$yearMonth-${"%02d".format(d)}"
                    if (DateHelper.isDateMatchingRepeat(
                            date = day,
                            repeatType = card.repeatType,
                            createdDate = createdDate,
                            daysOfWeek = card.repeatDaysOfWeek,
                            endDate = card.repeatEndDate,
                            skipDates = parseSkipDates(card.repeatSkipDates)
                        )
                    ) {
                        datesInMonth.add(day)
                    }
                }
            }

            val time = card.taskSetTimeStart?.let { DateHelper.getTimePart(it) }
            for (date in datesInMonth.sorted()) {
                val isFinished = card.finished && card.dateCompleted == date
                result.add(if (time != null) {
                    card.copy(taskSetTimeStart = "$date${'T'}$time", finished = isFinished, dateCompleted = if (isFinished) date else null)
                } else {
                    card.copy(taskSetTimeStart = date, finished = isFinished, dateCompleted = if (isFinished) date else null)
                })
            }
        }
        return result
    }

    fun deleteCompletedCard(id: Long) {
        _completedCards.update { list -> list.filterNot { it.id == id } }
        persistCompletedAsync()
    }

    fun addChecklistItem(cardId: Long, text: String) {
        _cards.update { list ->
            list.map { card ->
                if (card.id == cardId) {
                    val newItem = ChecklistItem(id = UUID.randomUUID().toString(), text = text.trim())
                    card.copy(checklist = card.checklist + newItem)
                } else card
            }
        }
        persistAsync()
    }

    fun removeChecklistItem(cardId: Long, itemId: String) {
        _cards.update { list ->
            list.map { card ->
                if (card.id == cardId) {
                    card.copy(checklist = card.checklist.filterNot { it.id == itemId })
                } else card
            }
        }
        persistAsync()
    }

    fun toggleChecklistItem(cardId: Long, itemId: String) {
        _cards.update { list ->
            list.map { card ->
                if (card.id == cardId) {
                    card.copy(checklist = card.checklist.map { item ->
                        if (item.id == itemId) item.copy(checked = !item.checked)
                        else item
                    })
                } else card
            }
        }
        persistAsync()
    }

    fun updateChecklistItemText(cardId: Long, itemId: String, text: String) {
        _cards.update { list ->
            list.map { card ->
                if (card.id == cardId) {
                    card.copy(checklist = card.checklist.map { item ->
                        if (item.id == itemId) item.copy(text = text.trim())
                        else item
                    })
                } else card
            }
        }
        persistAsync()
    }

    fun setTagFilter(tagId: String?) {
        _activeTagFilter.value = tagId
    }

    fun toggleCardTag(cardId: Long, tagId: String) {
        _cards.update { list ->
            list.map { card ->
                if (card.id == cardId) {
                    card.copy(tagIds = if (card.tagIds.contains(tagId)) {
                        card.tagIds - tagId
                    } else {
                        card.tagIds + tagId
                    })
                } else card
            }
        }
        persistAsync()
    }

    fun addTag(name: String, colorIndex: Int): String {
        val id = UUID.randomUUID().toString()
        _tags.update { it + CardTag(id = id, name = name.trim(), colorIndex = colorIndex) }
        persistTags()
        return id
    }

    fun deleteTag(tagId: String) {
        _tags.update { it.filterNot { t -> t.id == tagId } }
        _cards.update { list ->
            list.map { card -> card.copy(tagIds = card.tagIds - tagId) }
        }
        if (_activeTagFilter.value == tagId) {
            _activeTagFilter.value = null
        }
        persistAsync()
        persistTags()
    }

    fun updateTag(tagId: String, name: String, colorIndex: Int) {
        _tags.update { list ->
            list.map { if (it.id == tagId) it.copy(name = name.trim(), colorIndex = colorIndex) else it }
        }
        persistTags()
    }

    fun updateSettings(newSettings: Settings) {
        _settings.value = newSettings
        saveSettings()
    }

    fun checkYesterdayUncompleted() {
        val todayDate = DateHelper.todayDate()
        val lastShown = prefs.getString("yesterday_dialog_shown_date", null)
        if (lastShown == todayDate) return

        val yesterdayDate = DateHelper.addDays(todayDate, -1)
        val uncompleted = _cards.value.filter { card ->
            !card.finished && card.taskSetTimeStart?.startsWith(yesterdayDate) == true
        }
        if (uncompleted.isNotEmpty()) {
            _yesterdayUncompleted.value = uncompleted
            _showYesterdayDialog.value = true
        }
    }

    fun dismissYesterdayDialog() {
        _showYesterdayDialog.value = false
        prefs.edit().putString("yesterday_dialog_shown_date", DateHelper.todayDate()).apply()
    }

    fun checkYesterdayCard(card: CardItem) {
        toggleFinished(card)
        _yesterdayUncompleted.value = _yesterdayUncompleted.value.filterNot { it.id == card.id }
        if (_yesterdayUncompleted.value.isEmpty()) {
            dismissYesterdayDialog()
        }
    }

    private fun computeTriggerTime(card: CardItem, reminder: CardReminder): Long? {
        if (reminder.customTime != null) {
            return try {
                val cal = DateHelper.parseDateTime(reminder.customTime)
                if (cal.before(DateHelper.nowCal())) {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    if (cal.before(DateHelper.nowCal())) null else cal.timeInMillis
                } else cal.timeInMillis
            } catch (e: Exception) {
                Log.e("Reminder", "computeTriggerTime custom parse failed for card ${card.id}: $e")
                null
            }
        }
        val minutesBefore = reminder.minutesBefore ?: return null
        val start = card.taskSetTimeStart ?: return null
        return try {
            val cal = if (start.contains("T")) {
                DateHelper.parseDateTime(start)
            } else {
                val s = _settings.value
                DateHelper.parseDate(start).apply {
                    set(Calendar.HOUR_OF_DAY, s.defaultReminderHour)
                    set(Calendar.MINUTE, s.defaultReminderMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
            cal.add(Calendar.MINUTE, -minutesBefore)
            if (cal.before(DateHelper.nowCal())) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                if (cal.before(DateHelper.nowCal())) null else cal.timeInMillis
            } else cal.timeInMillis
        } catch (e: Exception) {
            Log.e("Reminder", "computeTriggerTime parse failed for card ${card.id}: $e")
            null
        }
    }

    private fun scheduleReminder(card: CardItem) {
        if (card.finished) {
            Log.d("Reminder", "scheduleReminder: card ${card.id} is finished, skipping")
            return
        }
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for ((index, reminder) in card.reminders.withIndex()) {
            val triggerAt = computeTriggerTime(card, reminder)
            if (triggerAt == null) {
                Log.d("Reminder", "scheduleReminder: card ${card.id} reminder $index has no trigger time, skipping")
                continue
            }
            val requestCode = ((card.id % 100000) * 10 + index).toInt()
            Log.d("Reminder", "scheduleReminder: card ${card.id} reminder $index, triggerAt=$triggerAt, requestCode=$requestCode")
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(NotificationHelper.EXTRA_CARD_ID, card.id)
                putExtra(NotificationHelper.EXTRA_CARD_NAME, card.name)
                putExtra(NotificationHelper.EXTRA_CARD_DESC, card.description)
                putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            }
            val pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms()
            ) {
                try {
                    Log.d("Reminder", "scheduleReminder: using setAlarmClock")
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, null), pi)
                    Log.d("Reminder", "scheduleReminder: setAlarmClock succeeded")
                    continue
                } catch (e: Exception) {
                    Log.e("Reminder", "scheduleReminder: setAlarmClock failed for card ${card.id}: $e")
                }
            }
            try {
                Log.d("Reminder", "scheduleReminder: using setWindow (fallback)")
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000, pi)
                Log.d("Reminder", "scheduleReminder: setWindow succeeded")
            } catch (e: Exception) {
                Log.e("Reminder", "scheduleReminder: setWindow also failed for card ${card.id}: $e")
            }
        }
    }

    private fun cancelReminder(card: CardItem) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in card.reminders.indices) {
            val requestCode = ((card.id % 100000) * 10 + index).toInt()
            val intent = Intent(context, ReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun runDailyScanIfNeeded() {
        val s = _settings.value
        val nowCal = DateHelper.nowCal()
        val todayDate = DateHelper.todayDate()
        val lastScan = prefs.getString("last_scan_date", null)

        val dayStartTodayCal = DateHelper.parseDateTime(
            DateHelper.atTime(todayDate, s.dayStartsHour, s.dayStartsMinute)
        )

        if (lastScan != todayDate && !nowCal.before(dayStartTodayCal)) {
            val renewed = mutableListOf<CardItem>()
            val remaining = mutableListOf<CardItem>()

            for (card in _cards.value) {
                if (card.repeatType != RepeatType.NONE && card.finished && card.dateCompleted != null && card.dateCompleted < todayDate) {
                    val renewedCard = card.copy(
                        finished = false,
                        dateCompleted = null,
                        checklist = emptyList()
                    )
                    renewed.add(renewedCard)
                    scheduleReminder(renewedCard)
                } else {
                    remaining.add(card)
                }
            }

            if (renewed.isNotEmpty()) {
                _cards.value = remaining + renewed
                persistAsync()
            }
            prefs.edit().putString("last_scan_date", todayDate).apply()
        }
    }

    private fun parseSkipDates(json: String?): List<String>? {
        if (json == null) return null
        return try {
            val arr = gson.fromJson(json, Array<String>::class.java)
            arr?.toList()
        } catch (_: Exception) { null }
    }

    private fun loadTags() {
        val json = prefs.getString("tags_json", null) ?: return
        try {
            val type = object : TypeToken<List<CardTag>>() {}.type
            val loaded: List<CardTag>? = gson.fromJson(json, type)
            if (loaded != null) _tags.value = loaded
        } catch (_: Exception) {}
    }

    private fun persistTags() {
        prefs.edit().putString("tags_json", gson.toJson(_tags.value)).apply()
    }

    private fun loadFromPrefs(): List<CardItem> {
        val json = prefs.getString("cards_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CardItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadCompletedFromPrefs(): List<CardItem> {
        val json = prefs.getString("completed_cards_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CardItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadSettings() {
        val hour = prefs.getInt("settings_day_starts_hour", 3)
        val minute = prefs.getInt("settings_day_starts_minute", 0)
        val themeModeStr = prefs.getString("settings_theme_mode", ThemeMode.SYSTEM.name)
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
        val remHour = prefs.getInt("settings_default_reminder_hour", 9)
        val remMinute = prefs.getInt("settings_default_reminder_minute", 0)
        val colorThemeStr = prefs.getString("settings_color_theme", ColorTheme.BLUE.name)
        val colorTheme = try {
            ColorTheme.valueOf(colorThemeStr ?: ColorTheme.BLUE.name)
        } catch (_: Exception) {
            ColorTheme.BLUE
        }
        _settings.value = Settings(hour, minute, themeMode, colorTheme, remHour, remMinute)
    }

    private fun persistAsync() {
        val json = gson.toJson(_cards.value)
        prefs.edit().putString("cards_json", json).apply()
    }

    private fun persistCompletedAsync() {
        val json = gson.toJson(_completedCards.value)
        prefs.edit().putString("completed_cards_json", json).apply()
    }

    private fun saveSettings() {
        val s = _settings.value
        prefs.edit()
            .putInt("settings_day_starts_hour", s.dayStartsHour)
            .putInt("settings_day_starts_minute", s.dayStartsMinute)
            .putString("settings_theme_mode", s.themeMode.name)
            .putString("settings_color_theme", s.colorTheme.name)
            .putInt("settings_default_reminder_hour", s.defaultReminderHour)
            .putInt("settings_default_reminder_minute", s.defaultReminderMinute)
            .apply()
    }

    private fun migrateRemindersPrefs(key: String) {
        val json = prefs.getString(key, null) ?: return
        if (!json.contains("reminderMinutesBefore")) return

        val root: JsonElement = try {
            gson.fromJson(json, JsonElement::class.java)
        } catch (_: Exception) { return }
        if (!root.isJsonArray) return

        var changed = false
        for (element in root.asJsonArray) {
            val obj = element.asJsonObject
            if (!obj.has("reminderMinutesBefore") && !obj.has("reminderCustomTime")) continue
            val minutesBefore = if (obj.has("reminderMinutesBefore") && !obj.get("reminderMinutesBefore").isJsonNull)
                obj.get("reminderMinutesBefore").asInt else null
            val customTime = if (obj.has("reminderCustomTime") && !obj.get("reminderCustomTime").isJsonNull)
                obj.get("reminderCustomTime").asString else null

            val reminderObj = JsonObject()
            if (minutesBefore != null) reminderObj.addProperty("minutesBefore", minutesBefore)
            if (customTime != null) reminderObj.addProperty("customTime", customTime)
            val remindersArray = JsonArray()
            remindersArray.add(reminderObj)
            obj.add("reminders", remindersArray)

            obj.remove("reminderMinutesBefore")
            obj.remove("reminderCustomTime")
            changed = true
        }

        if (changed) {
            prefs.edit().putString(key, gson.toJson(root)).apply()
        }
    }
}
