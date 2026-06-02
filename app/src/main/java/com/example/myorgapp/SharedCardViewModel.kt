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
import androidx.lifecycle.viewModelScope
import java.util.Calendar
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SharedCardViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("card_pref", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _cards = MutableStateFlow<List<CardItem>>(emptyList())
    val cards: StateFlow<List<CardItem>> = _cards

    private val _completedCards = MutableStateFlow<List<CardItem>>(emptyList())
    val completedCards: StateFlow<List<CardItem>> = _completedCards

    private val _snoozedCards = MutableStateFlow<List<CardItem>>(emptyList())
    val snoozedCards: StateFlow<List<CardItem>> = _snoozedCards

    private val _trashedCards = MutableStateFlow<List<CardItem>>(emptyList())
    val trashedCards: StateFlow<List<CardItem>> = _trashedCards

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

    fun clearToast() {
        _toastMessage.value = null
    }

    private val _tags = MutableStateFlow<List<CardTag>>(emptyList())
    val tags: StateFlow<List<CardTag>> = _tags

    private val _activeTagFilter = MutableStateFlow<String?>(null)
    val activeTagFilter: StateFlow<String?> = _activeTagFilter

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    private val _highlightedCardId = MutableStateFlow<Long?>(null)

    val highlightedCardId: StateFlow<Long?> = _highlightedCardId

    private val _cardOrder = MutableStateFlow<List<Long>>(emptyList())
    val cardOrder: StateFlow<List<Long>> = _cardOrder

    private val _sortMode = MutableStateFlow("auto")
    val sortMode: StateFlow<String> = _sortMode

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var nextId = 0L

    init {
        loadSettings()
        viewModelScope.launch(Dispatchers.IO) {
            migrateRemindersPrefs("cards_json")
            migrateRemindersPrefs("completed_cards_json")
            loadTags()
            val saved = loadFromPrefs()
            val completed = loadCompletedFromPrefs()
            val snoozed = loadSnoozedFromPrefs()
            val trashed = loadTrashedFromPrefs()
            withContext(Dispatchers.Main) {
                _cards.value = saved
                nextId = (saved.maxOfOrNull { it.id } ?: 0L) + 1L
                _completedCards.value = completed
                _snoozedCards.value = snoozed
                _trashedCards.value = trashed
                _cardOrder.value = loadCardOrder()
                _sortMode.value = prefs.getString("sort_mode", "auto") ?: "auto"
                checkYesterdayUncompleted()
                runDailyScanIfNeeded()
                _isLoading.value = false
            }
        }
    }

    fun setEditing(card: CardItem?) {
        _editing.value = if (card == null) null
        else _cards.value.find { it.id == card.id } ?: card
    }

    fun setHighlightedCardId(id: Long?) {
        _highlightedCardId.value = id
    }

    fun setSortMode(mode: String) {
        _sortMode.value = mode
        prefs.edit().putString("sort_mode", mode).apply()
    }

    fun moveCardUp(cardId: Long) {
        val order = _cardOrder.value.toMutableList()
        val idx = order.indexOf(cardId)
        if (idx > 0) {
            order.removeAt(idx)
            order.add(idx - 1, cardId)
            _cardOrder.value = order
            saveCardOrder()
        }
    }

    fun moveCardDown(cardId: Long) {
        val order = _cardOrder.value.toMutableList()
        val idx = order.indexOf(cardId)
        if (idx >= 0 && idx < order.lastIndex) {
            order.removeAt(idx)
            order.add(idx + 1, cardId)
            _cardOrder.value = order
            saveCardOrder()
        }
    }

    fun addCardToOrder(cardId: Long) {
        val order = _cardOrder.value.toMutableList()
        if (cardId !in order) {
            order.add(cardId)
            _cardOrder.value = order
            saveCardOrder()
        }
    }

    fun removeCardFromOrder(cardId: Long) {
        val order = _cardOrder.value.toMutableList()
        if (order.remove(cardId)) {
            _cardOrder.value = order
            saveCardOrder()
        }
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
        tagIds: List<String> = emptyList(),
        priority: Int = 0,
        repeatCustomFrequency: String = "weekly",
        repeatDayOfMonth: Int? = null,
        repeatMonth: Int? = null
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
            tagIds = tagIds,
            priority = priority,
            repeatCustomFrequency = repeatCustomFrequency,
            repeatDayOfMonth = repeatDayOfMonth,
            repeatMonth = repeatMonth
        )
        _cards.update { it + card }
        scheduleReminder(card)
        persistAsync()
        addCardToOrder(card.id)
        updateStreak()
    }

    fun updateCard(updated: CardItem) {
        val old = _cards.value.find { it.id == updated.id }
        old?.let { cancelReminder(it) }
        _cards.update { list -> list.map { if (it.id == updated.id) updated else it } }
        scheduleReminder(updated)
        persistAsync()
        updateStreak()
    }

    fun deleteCard(id: Long) {
        val card = _cards.value.find { it.id == id } ?: return
        if (card.trashed) {
            permanentlyDeleteCard(card)
            return
        }
        cancelReminder(card)
        val trashed = card.copy(trashed = true, finished = false, dateCompleted = null)
        _cards.update { list -> list.filterNot { it.id == id } }
        _trashedCards.update { list -> list + trashed }
        persistAsync()
        persistTrashedAsync()
        removeCardFromOrder(id)
        updateStreak()
    }

    private fun permanentlyDeleteCard(card: CardItem) {
        cancelReminder(card)
        _trashedCards.update { list -> list.filterNot { it.id == card.id } }
        persistTrashedAsync()
        updateStreak()
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
            updateStreak()
            return
        }

        val completed = _completedCards.value.find { it.id == card.id } ?: return
        val restored = completed.copy(finished = false, dateCompleted = null)
        _completedCards.update { list -> list.filterNot { it.id == completed.id } }
        _cards.update { list -> list + restored }
        persistAsync()
        persistCompletedAsync()
        updateStreak()
    }

    fun getCardsForDate(dateStr: String): List<CardItem> {
        val result = mutableListOf<CardItem>()
        for (card in _cards.value) {
            if (card.snoozed || card.trashed) continue
            val timeMatch = card.taskSetTimeStart?.let { start ->
                try {
                    val startDate = DateHelper.getDatePart(start)
                    val endDate = card.taskSetTimeEnd?.let { DateHelper.getDatePart(it) }
                    startDate == dateStr || endDate == dateStr
                } catch (_: Exception) { false }
            } ?: false
            if (timeMatch) {
                val isFinished = card.finished && card.dateCompleted == dateStr
                val startDate = card.taskSetTimeStart?.let { DateHelper.getDatePart(it) }
                val endDate = card.taskSetTimeEnd?.let { DateHelper.getDatePart(it) }
                if (endDate == dateStr && startDate != null && startDate != endDate) {
                    val endTime = DateHelper.getTimePart(card.taskSetTimeEnd)
                    result.add(card.copy(
                        taskSetTimeStart = "$dateStr${'T'}$endTime",
                        taskSetTimeEnd = null,
                        finished = isFinished,
                        dateCompleted = if (isFinished) dateStr else null
                    ))
                } else {
                    result.add(card.copy(finished = isFinished, dateCompleted = if (isFinished) dateStr else null))
                }
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
                    skipDates = parseSkipDates(card.repeatSkipDates),
                    repeatCustomFrequency = card.repeatCustomFrequency,
                    repeatDayOfMonth = card.repeatDayOfMonth,
                    repeatMonth = card.repeatMonth
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
            if (card.snoozed || card.trashed) continue
            val datesInWeek = mutableSetOf<String>()

            card.taskSetTimeStart?.let { start ->
                try {
                    val sd = DateHelper.getDatePart(start)
                    if (sd >= startOfWeek && sd <= endOfWeek) {
                        datesInWeek.add(sd)
                    }
                } catch (_: Exception) {}
            }

            card.taskSetTimeEnd?.let { end ->
                try {
                    val ed = DateHelper.getDatePart(end)
                    if (ed >= startOfWeek && ed <= endOfWeek) {
                        datesInWeek.add(ed)
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
                            skipDates = parseSkipDates(card.repeatSkipDates),
                            repeatCustomFrequency = card.repeatCustomFrequency,
                            repeatDayOfMonth = card.repeatDayOfMonth,
                            repeatMonth = card.repeatMonth
                        )
                    ) {
                        datesInWeek.add(day)
                    }
                    day = DateHelper.addDays(day, 1)
                }
            }

            val time = card.taskSetTimeStart?.let { DateHelper.getTimePart(it) }
            val endTime = card.taskSetTimeEnd?.let { DateHelper.getTimePart(it) }
            val startDate = card.taskSetTimeStart?.let { DateHelper.getDatePart(it) }
            for (date in datesInWeek.sorted()) {
                val isFinished = card.finished && card.dateCompleted == date
                val useEndTime = date != startDate && startDate != null && endTime != null
                val dateTime = if (useEndTime) endTime else time
                result.add(if (dateTime != null) {
                    card.copy(
                        taskSetTimeStart = "$date${'T'}$dateTime",
                        taskSetTimeEnd = if (useEndTime) null else card.taskSetTimeEnd,
                        finished = isFinished,
                        dateCompleted = if (isFinished) date else null
                    )
                } else {
                    card.copy(
                        taskSetTimeStart = date,
                        taskSetTimeEnd = if (useEndTime) null else card.taskSetTimeEnd,
                        finished = isFinished,
                        dateCompleted = if (isFinished) date else null
                    )
                })
            }
        }
        return result
    }

    fun getCardsForMonth(yearMonth: String): List<CardItem> {
        val result = mutableListOf<CardItem>()
        for (card in _cards.value) {
            if (card.snoozed || card.trashed) continue
            val datesInMonth = mutableSetOf<String>()

            card.taskSetTimeStart?.let { start ->
                try {
                    if (DateHelper.getYearMonth(start) == yearMonth) {
                        datesInMonth.add(DateHelper.getDatePart(start))
                    }
                } catch (_: Exception) {}
            }

            card.taskSetTimeEnd?.let { end ->
                try {
                    if (DateHelper.getYearMonth(end) == yearMonth) {
                        datesInMonth.add(DateHelper.getDatePart(end))
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
                            skipDates = parseSkipDates(card.repeatSkipDates),
                            repeatCustomFrequency = card.repeatCustomFrequency,
                            repeatDayOfMonth = card.repeatDayOfMonth,
                            repeatMonth = card.repeatMonth
                        )
                    ) {
                        datesInMonth.add(day)
                    }
                }
            }

            val time = card.taskSetTimeStart?.let { DateHelper.getTimePart(it) }
            val endTime = card.taskSetTimeEnd?.let { DateHelper.getTimePart(it) }
            val startDate = card.taskSetTimeStart?.let { DateHelper.getDatePart(it) }
            for (date in datesInMonth.sorted()) {
                val isFinished = card.finished && card.dateCompleted == date
                val useEndTime = date != startDate && startDate != null && endTime != null
                val dateTime = if (useEndTime) endTime else time
                result.add(if (dateTime != null) {
                    card.copy(
                        taskSetTimeStart = "$date${'T'}$dateTime",
                        taskSetTimeEnd = if (useEndTime) null else card.taskSetTimeEnd,
                        finished = isFinished,
                        dateCompleted = if (isFinished) date else null
                    )
                } else {
                    card.copy(
                        taskSetTimeStart = date,
                        taskSetTimeEnd = if (useEndTime) null else card.taskSetTimeEnd,
                        finished = isFinished,
                        dateCompleted = if (isFinished) date else null
                    )
                })
            }
        }
        return result
    }

    fun deleteCompletedCard(id: Long) {
        val card = _completedCards.value.find { it.id == id } ?: return
        if (card.trashed) {
            permanentlyDeleteCompletedCard(card)
            return
        }
        val trashed = card.copy(trashed = true)
        _completedCards.update { list -> list.filterNot { it.id == id } }
        _trashedCards.update { list -> list + trashed }
        persistCompletedAsync()
        persistTrashedAsync()
    }

    private fun permanentlyDeleteCompletedCard(card: CardItem) {
        _trashedCards.update { list -> list.filterNot { it.id == card.id } }
        persistTrashedAsync()
    }

    fun snoozeCard(card: CardItem) {
        val original = _cards.value.find { it.id == card.id } ?: return
        cancelReminder(original)
        val snoozed = original.copy(snoozed = true, finished = false, dateCompleted = null)
        _cards.update { list -> list.filterNot { it.id == card.id } }
        _snoozedCards.update { list -> list + snoozed }
        persistAsync()
        persistSnoozedAsync()
        removeCardFromOrder(card.id)
        updateStreak()
    }

    fun unsnoozeCard(id: Long) {
        val card = _snoozedCards.value.find { it.id == id } ?: return
        val restored = card.copy(snoozed = false, priority = 0)
        _snoozedCards.update { list -> list.filterNot { it.id == id } }
        _cards.update { list -> list + restored }
        persistSnoozedAsync()
        persistAsync()
        addCardToOrder(id)
        updateStreak()
    }

    fun trashSnoozedCard(id: Long) {
        val card = _snoozedCards.value.find { it.id == id } ?: return
        val trashed = card.copy(trashed = true)
        _snoozedCards.update { list -> list.filterNot { it.id == id } }
        _trashedCards.update { list -> list + trashed }
        persistSnoozedAsync()
        persistTrashedAsync()
    }

    fun permanentlyDeleteFromTrash(id: Long) {
        _trashedCards.update { list -> list.filterNot { it.id == id } }
        persistTrashedAsync()
    }

    fun restoreFromTrash(id: Long) {
        val card = _trashedCards.value.find { it.id == id } ?: return
        val restored = card.copy(trashed = false, snoozed = false, priority = 0, finished = false, dateCompleted = null)
        _trashedCards.update { list -> list.filterNot { it.id == id } }
        _cards.update { list -> list + restored }
        persistTrashedAsync()
        persistAsync()
        addCardToOrder(id)
        updateStreak()
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
            !card.snoozed && !card.trashed && !card.finished && card.taskSetTimeStart?.startsWith(yesterdayDate) == true
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
        val yesterday = DateHelper.addDays(DateHelper.todayDate(), -1)
        val original = _cards.value.find { it.id == card.id }
        if (original != null) {
            if (original.repeatType != RepeatType.NONE) {
                updateCard(original.copy(
                    finished = true,
                    dateCompleted = yesterday,
                    repeatCompletionCount = original.repeatCompletionCount + 1
                ))
            } else {
                cancelReminder(original)
                _cards.update { list -> list.filterNot { it.id == original.id } }
                _completedCards.update { list -> list + original.copy(finished = true, dateCompleted = yesterday) }
                persistAsync()
                persistCompletedAsync()
            }
        }
        _yesterdayUncompleted.value = _yesterdayUncompleted.value.filterNot { it.id == card.id }
        if (_yesterdayUncompleted.value.isEmpty()) {
            dismissYesterdayDialog()
        }
        updateStreak()
    }

    private fun calculateStreak(): Int {
        val today = DateHelper.todayDate()
        var consecutive = 0
        var d = today
        while (true) {
            val active = getCardsForDate(d)
            val completedOnDay = _completedCards.value.filter { it.dateCompleted == d }
            if (active.isEmpty() && completedOnDay.isEmpty()) break
            if (active.any { !it.finished }) break
            consecutive++
            d = DateHelper.addDays(d, -1)
        }
        return maxOf(0, consecutive - 1)
    }

    fun updateStreak() {
        _streak.value = calculateStreak()
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
            val snoozed = mutableListOf<CardItem>()

            for (card in _cards.value) {
                if (card.snoozed || card.trashed) continue
                if (card.repeatType != RepeatType.NONE && card.finished && card.dateCompleted != null && card.dateCompleted < todayDate) {
                    val startTime = card.taskSetTimeStart?.let { DateHelper.getTimePart(it) }
                    val endTime = card.taskSetTimeEnd?.let { DateHelper.getTimePart(it) }
                    val renewedCard = card.copy(
                        finished = false,
                        dateCompleted = null,
                        taskSetTimeStart = if (startTime != null) "${todayDate}T$startTime" else todayDate,
                        taskSetTimeEnd = if (endTime != null) "${todayDate}T$endTime" else null,
                        checklist = card.checklist.map { it.copy(checked = false) }
                    )
                    renewed.add(renewedCard)
                    scheduleReminder(renewedCard)
                } else if (!card.finished && card.taskSetTimeStart != null) {
                    val cardDate = DateHelper.getDatePart(card.taskSetTimeStart)
                    if (cardDate < todayDate) {
                        val cardCal = DateHelper.parseDate(cardDate)
                        val daysOverdue = DateHelper.daysBetween(cardCal, DateHelper.nowCal())
                        when {
                            daysOverdue <= 1L -> {
                                cancelReminder(card)
                                val time = DateHelper.getTimePart(card.taskSetTimeStart)
                                val rescheduled = card.copy(
                                    priority = -1,
                                    taskSetTimeStart = if (time.isNotEmpty()) "${todayDate}T$time" else todayDate,
                                    taskSetTimeEnd = null
                                )
                                renewed.add(rescheduled)
                                scheduleReminder(rescheduled)
                            }
                            daysOverdue <= 7L -> {
                                remaining.add(card.copy(priority = -2))
                            }
                            else -> {
                                cancelReminder(card)
                                snoozed.add(card.copy(
                                    snoozed = true,
                                    priority = -3,
                                    finished = false,
                                    dateCompleted = null
                                ))
                            }
                        }
                    } else {
                        remaining.add(card)
                    }
                } else {
                    remaining.add(card)
                }
            }

            val changed = renewed.isNotEmpty() || snoozed.isNotEmpty()
            if (changed) {
                _cards.value = remaining + renewed
                _snoozedCards.value = _snoozedCards.value + snoozed
                persistAsync()
                if (snoozed.isNotEmpty()) persistSnoozedAsync()
            }
            prefs.edit().putString("last_scan_date", todayDate).apply()
            updateStreak()
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

    private fun loadCardOrder(): List<Long> {
        val json = prefs.getString("card_order", null) ?: return _cards.value.map { it.id }
        return try {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(json, type) ?: _cards.value.map { it.id }
        } catch (e: Exception) {
            _cards.value.map { it.id }
        }
    }

    private fun saveCardOrder() {
        prefs.edit().putString("card_order", gson.toJson(_cardOrder.value)).apply()
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

    private fun loadSnoozedFromPrefs(): List<CardItem> {
        val json = prefs.getString("snoozed_cards_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CardItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadTrashedFromPrefs(): List<CardItem> {
        val json = prefs.getString("trashed_cards_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CardItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistSnoozedAsync() {
        val json = gson.toJson(_snoozedCards.value)
        prefs.edit().putString("snoozed_cards_json", json).apply()
    }

    private fun persistTrashedAsync() {
        val json = gson.toJson(_trashedCards.value)
        prefs.edit().putString("trashed_cards_json", json).apply()
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
