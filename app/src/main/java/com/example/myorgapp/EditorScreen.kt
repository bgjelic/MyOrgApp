package com.example.myorgapp

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import java.util.Calendar
import java.util.UUID
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private fun datePart(s: String?): String = s?.substringBefore("T") ?: ""
private fun timePart(s: String?): String = s?.substringAfter("T", "") ?: ""

private fun parseDateToMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        DateHelper.dateToMillis(dateStr)
    } catch (_: Exception) { null }
}

private fun parseHour(dateTimeStr: String?): Int {
    val t = timePart(dateTimeStr)
    return try { t.substringBefore(":").toInt().coerceIn(0, 23) } catch (_: Exception) { 12 }
}

private fun parseMinute(dateTimeStr: String?): Int {
    val t = timePart(dateTimeStr)
    return try { t.substringAfter(":").toInt().coerceIn(0, 59) } catch (_: Exception) { 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: SharedCardViewModel, onDone: () -> Unit) {
    val editing by viewModel.editing.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var nameHasError by remember { mutableStateOf(false) }
    val defaultStart = remember {
        "${DateHelper.todayDate()}T${DateHelper.formatTime(DateHelper.nowCal())}"
    }
    val defaultEnd = remember {
        val cal = DateHelper.nowCal()
        cal.add(Calendar.HOUR_OF_DAY, 1)
        "${DateHelper.todayDate()}T${DateHelper.formatTime(cal)}"
    }
    val defaultReminders = remember { listOf(CardReminder(minutesBefore = 15)) }
    var taskSetTimeStart by remember { mutableStateOf(editing?.taskSetTimeStart ?: defaultStart) }
    var taskSetTimeEnd by remember { mutableStateOf(editing?.taskSetTimeEnd ?: defaultEnd) }
    var cardReminders by remember { mutableStateOf(editing?.reminders ?: defaultReminders) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showCustomReminderDatePicker by remember { mutableStateOf(false) }
    var showCustomReminderTimePicker by remember { mutableStateOf(false) }
    var tempCustomReminderDate by remember { mutableStateOf("") }

    val initialFrequency = when (editing?.repeatType) {
        RepeatType.MONTHLY -> "monthly"
        RepeatType.YEARLY -> "yearly"
        RepeatType.WEEKDAYS, RepeatType.WEEKENDS -> "weekly"
        else -> editing?.repeatCustomFrequency ?: "weekly"
    }
    val initialDayOfMonth = editing?.repeatDayOfMonth ?: (editing?.let {
        if (it.repeatType == RepeatType.MONTHLY) DateHelper.parseDate(it.dateCreated ?: DateHelper.todayDate()).get(Calendar.DAY_OF_MONTH)
        else null
    })
    val initialMonth = editing?.repeatMonth ?: (editing?.let {
        if (it.repeatType == RepeatType.YEARLY) (DateHelper.parseDate(it.dateCreated ?: DateHelper.todayDate()).get(Calendar.MONTH) + 1)
        else null
    })
    var repeatType by remember { mutableStateOf(editing?.repeatType ?: RepeatType.DAILY) }
    var repeatDaysOfWeek by remember { mutableStateOf(editing?.repeatDaysOfWeek) }
    var repeatEndDate by remember { mutableStateOf(editing?.repeatEndDate ?: "") }
    var repeatSkipDates by remember { mutableStateOf(editing?.repeatSkipDates ?: "") }
    var repeatCustomFrequency by remember { mutableStateOf(initialFrequency) }
    var repeatDayOfMonth by remember { mutableStateOf<Int?>(initialDayOfMonth ?: 1) }
    var repeatMonth by remember { mutableStateOf<Int?>(initialMonth ?: 1) }

    var showCustomRepeatDialog by remember { mutableStateOf(false) }

    var checklistItems by remember { mutableStateOf(editing?.checklist ?: emptyList()) }
    var newChecklistText by remember { mutableStateOf("") }

    val tags by viewModel.tags.collectAsState()
    var cardTagIds by remember { mutableStateOf(editing?.tagIds ?: emptyList()) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColorIndex by remember { mutableStateOf(0) }

    var priority by remember { mutableStateOf(maxOf(editing?.priority ?: 0, 0)) }

    val existingCards by viewModel.cards.collectAsState()
    val completedCards by viewModel.completedCards.collectAsState()
    var nameFocused by remember { mutableStateOf(false) }

    val nameSuggestions = remember(name, nameFocused, existingCards, completedCards) {
        if (name.isBlank() || !nameFocused) emptyList()
        else (existingCards + completedCards)
            .filter { it.name.contains(name, ignoreCase = true) && !it.name.equals(name, ignoreCase = true) }
            .distinctBy { it.name }
            .take(8)
    }

    val nameLabel = stringResource(R.string.name)
    val nameRequiredLabel = stringResource(R.string.name_required)
    val descLabel = stringResource(R.string.description)
    val pickDateLabel = stringResource(R.string.pick_date)
    val taskStartLabel = stringResource(R.string.task_start)
    val taskEndLabel = stringResource(R.string.task_end)
    val pickTimeLabel = stringResource(R.string.pick_time)
    val selectTimeLabel = stringResource(R.string.select_time)
    val saveLabel = stringResource(R.string.save)
    val cancelLabel = stringResource(R.string.cancel)
    val deleteLabel = stringResource(R.string.delete)
    val okLabel = stringResource(R.string.ok)
    val addCardLabel = stringResource(R.string.add_card)
    val reminderLabel = stringResource(R.string.reminder)
    val reminderNoneLabel = stringResource(R.string.reminder_none)
    val reminderAtTimeLabel = stringResource(R.string.reminder_at_time)
    val reminder5minLabel = stringResource(R.string.reminder_5min)
    val reminder15minLabel = stringResource(R.string.reminder_15min)
    val reminder30minLabel = stringResource(R.string.reminder_30min)
    val reminder1hrLabel = stringResource(R.string.reminder_1hr)
    val reminder2hrLabel = stringResource(R.string.reminder_2hr)
    val reminder1dayLabel = stringResource(R.string.reminder_1day)
    val selectReminderLabel = stringResource(R.string.select_reminder)
    val reminderCustomLabel = stringResource(R.string.reminder_custom)
    val reminderCustomDateLabel = stringResource(R.string.reminder_custom_date)
    val reminderCustomTimeLabel = stringResource(R.string.reminder_custom_time)

    val repeatLabel = stringResource(R.string.repeat)
    val repeatNoneLabel = stringResource(R.string.repeat_none)
    val repeatDailyLabel = stringResource(R.string.repeat_daily)
    val repeatWeeklyLabel = stringResource(R.string.repeat_weekly)
    val repeatMonthlyLabel = stringResource(R.string.repeat_monthly)
    val repeatYearlyLabel = stringResource(R.string.repeat_yearly)
    val repeatCustomLabel = stringResource(R.string.repeat_custom)
    val selectRepeatLabel = stringResource(R.string.select_repeat)
    val customRepeatTitleLabel = stringResource(R.string.custom_repeat_title)
    val repeatOnDaysLabel = stringResource(R.string.repeat_on_days)
    val repeatEndDateLabel = stringResource(R.string.repeat_end_date)
    val repeatSkipDateLabel = stringResource(R.string.repeat_skip_date)
    val repeatNeverEndsLabel = stringResource(R.string.repeat_never_ends)
    val repeatUntilLabel = stringResource(R.string.repeat_until)

    val dayMonLabel = stringResource(R.string.day_mon)
    val dayTueLabel = stringResource(R.string.day_tue)
    val dayWedLabel = stringResource(R.string.day_wed)
    val dayThuLabel = stringResource(R.string.day_thu)
    val dayFriLabel = stringResource(R.string.day_fri)
    val daySatLabel = stringResource(R.string.day_sat)
    val daySunLabel = stringResource(R.string.day_sun)

    LaunchedEffect(editing?.id) {
        if (editing == null) {
            name = ""
            description = ""
            taskSetTimeStart = defaultStart
            taskSetTimeEnd = defaultEnd
            cardReminders = defaultReminders
            repeatType = RepeatType.DAILY
            repeatDaysOfWeek = null
            repeatEndDate = ""
            repeatSkipDates = ""
            repeatCustomFrequency = "weekly"
            repeatDayOfMonth = 1
            repeatMonth = 1
            checklistItems = emptyList()
            newChecklistText = ""
            cardTagIds = emptyList()
            priority = 0
        } else {
            name = editing?.name ?: ""
            description = editing?.description ?: ""
            taskSetTimeStart = editing?.taskSetTimeStart ?: ""
            taskSetTimeEnd = editing?.taskSetTimeEnd ?: ""
            cardReminders = editing?.reminders ?: emptyList()
            repeatType = editing?.repeatType ?: RepeatType.DAILY
            repeatDaysOfWeek = editing?.repeatDaysOfWeek
            repeatEndDate = editing?.repeatEndDate ?: ""
            repeatSkipDates = editing?.repeatSkipDates ?: ""
            repeatCustomFrequency = editing?.repeatCustomFrequency ?: "weekly"
            repeatDayOfMonth = editing?.repeatDayOfMonth ?: 1
            repeatMonth = editing?.repeatMonth ?: 1
            checklistItems = editing?.checklist ?: emptyList()
            newChecklistText = ""
            cardTagIds = editing?.tagIds ?: emptyList()
            priority = editing?.priority ?: 0
        }
    }

    LaunchedEffect(Unit) {
        if (editing == null) focusRequester.requestFocus()
    }

    fun saveCard() {
        if (name.isBlank()) {
            nameHasError = true
            return
        }
        if (editing == null) {
            viewModel.addCard(
                name = name.trim(),
                description = description.trim(),
                taskSetTimeStart = taskSetTimeStart.ifBlank { DateHelper.todayDate() },
                taskSetTimeEnd = taskSetTimeEnd.ifBlank { null },
                reminders = cardReminders,
                repeatType = repeatType,
                repeatDaysOfWeek = repeatDaysOfWeek,
                repeatEndDate = repeatEndDate.ifBlank { null },
                repeatSkipDates = repeatSkipDates.ifBlank { null },
                checklist = checklistItems,
                tagIds = cardTagIds,
                priority = priority,
                repeatCustomFrequency = repeatCustomFrequency,
                repeatDayOfMonth = repeatDayOfMonth,
                repeatMonth = repeatMonth
            )
        } else {
            val current = editing!!
            val updated = current.copy(
                name = name.trim(),
                description = description.trim(),
                taskSetTimeStart = taskSetTimeStart.ifBlank { DateHelper.todayDate() },
                taskSetTimeEnd = taskSetTimeEnd.ifBlank { null },
                reminders = cardReminders,
                repeatType = repeatType,
                repeatDaysOfWeek = repeatDaysOfWeek,
                repeatEndDate = repeatEndDate.ifBlank { null },
                repeatSkipDates = repeatSkipDates.ifBlank { null },
                checklist = checklistItems,
                tagIds = cardTagIds,
                priority = priority,
                repeatCustomFrequency = repeatCustomFrequency,
                repeatDayOfMonth = repeatDayOfMonth,
                repeatMonth = repeatMonth
            )
            viewModel.updateCard(updated)
        }
        onDone()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (editing == null) addCardLabel
                        else "$nameLabel: ${editing?.name?.take(20) ?: ""}"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameHasError = false },
                    label = { Text(nameLabel) },
                    isError = nameHasError,
                    supportingText = { if (nameHasError) Text(nameRequiredLabel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { f -> nameFocused = f.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { saveCard() }
                    )
                )
                DropdownMenu(
                    expanded = nameSuggestions.isNotEmpty(),
                    onDismissRequest = { nameFocused = false }
                ) {
                    nameSuggestions.forEach { card ->
                        DropdownMenuItem(
                            text = { Text("${card.name} (duplicate)") },
                            onClick = {
                                name = card.name
                                description = card.description
                                taskSetTimeStart = card.taskSetTimeStart ?: ""
                                taskSetTimeEnd = card.taskSetTimeEnd ?: ""
                                priority = 0
                                cardReminders = card.reminders
                                repeatType = card.repeatType
                                repeatDaysOfWeek = card.repeatDaysOfWeek
                                repeatEndDate = card.repeatEndDate ?: ""
                                repeatSkipDates = card.repeatSkipDates ?: ""
                                repeatCustomFrequency = card.repeatCustomFrequency
                                repeatDayOfMonth = card.repeatDayOfMonth ?: 1
                                repeatMonth = card.repeatMonth ?: 1
                                checklistItems = card.checklist
                                cardTagIds = card.tagIds
                                nameFocused = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(descLabel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                maxLines = 5
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Tags:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            if (tags.isEmpty()) {
                Text(
                    "No tags yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        val selected = tag.id in cardTagIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                cardTagIds = if (selected) cardTagIds - tag.id
                                else cardTagIds + tag.id
                            },
                            label = { Text(tag.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tagPalette[tag.colorIndex % tagPalette.size])
                                )
                            }
                        )
                    }
                }
            }
            TextButton(onClick = {
                newTagName = ""
                newTagColorIndex = 0
                showCreateTagDialog = true
            }) {
                Text("+ Create tag", style = MaterialTheme.typography.bodySmall)
            }

            if (editing != null && (editing?.priority ?: 0) < 0) {
                val warningText = when (editing?.priority) {
                    -1 -> "This card was auto-rescheduled (1 day overdue)"
                    -2 -> "This card needs a new date/time (overdue 2-7 days)"
                    -3 -> "This card was auto-snoozed (overdue 7+ days)"
                    else -> "This card has a pending action"
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Priority Goal:",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0 to "! Made it work",
                    1 to "★ Minimum (Lazy)",
                    2 to "★★ Normal (Disciplined)",
                    3 to "★★★ New level (Good)"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = priority == value,
                        onClick = { priority = value },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "$taskStartLabel:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showStartDatePicker = true }) {
                    Text(datePart(taskSetTimeStart).ifBlank { pickDateLabel })
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showStartTimePicker = true }) {
                    Text(timePart(taskSetTimeStart).ifBlank { pickTimeLabel })
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "$taskEndLabel:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showEndDatePicker = true }) {
                    Text(datePart(taskSetTimeEnd).ifBlank { pickDateLabel })
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showEndTimePicker = true }) {
                    Text(timePart(taskSetTimeEnd).ifBlank { pickTimeLabel })
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "$reminderLabel:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            cardReminders.forEach { reminder ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    val label = if (reminder.customTime != null) {
                        "Custom: ${reminder.customTime.replace("T", " ")}"
                    } else when (reminder.minutesBefore) {
                        null -> reminderNoneLabel
                        0 -> reminderAtTimeLabel
                        5 -> reminder5minLabel
                        15 -> reminder15minLabel
                        30 -> reminder30minLabel
                        60 -> reminder1hrLabel
                        120 -> reminder2hrLabel
                        1440 -> reminder1dayLabel
                        else -> "${reminder.minutesBefore} min"
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { cardReminders = cardReminders.filterNot { it.id == reminder.id } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove reminder")
                    }
                }
            }
            if (cardReminders.size < 5) {
                TextButton(onClick = { showReminderPicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add reminder", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "$repeatLabel:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = repeatType == RepeatType.DAILY,
                    onClick = {
                        repeatType = RepeatType.DAILY
                        repeatDaysOfWeek = null
                    },
                    label = { Text(repeatDailyLabel) }
                )
                FilterChip(
                    selected = repeatType == RepeatType.WEEKLY,
                    onClick = {
                        repeatType = RepeatType.WEEKLY
                        repeatDaysOfWeek = null
                    },
                    label = { Text(repeatWeeklyLabel) }
                )
                FilterChip(
                    selected = repeatType == RepeatType.CUSTOM,
                    onClick = {
                        if (repeatType != RepeatType.CUSTOM) {
                            repeatType = RepeatType.CUSTOM
                            repeatCustomFrequency = "weekly"
                            repeatDaysOfWeek = null
                            repeatDayOfMonth = 1
                            repeatMonth = 1
                        }
                        showCustomRepeatDialog = true
                    },
                    label = {
                        val txt = if (repeatType == RepeatType.CUSTOM) {
                            when (repeatCustomFrequency) {
                                "biweekly" -> "Every 2 weeks"
                                "monthly" -> repeatMonthlyLabel
                                "yearly" -> repeatYearlyLabel
                                else -> {
                                    val dayCount = repeatDaysOfWeek?.size ?: 0
                                    if (dayCount > 0) "$repeatWeeklyLabel ($dayCount)"
                                    else repeatWeeklyLabel
                                }
                            }
                        } else repeatCustomLabel
                        Text(txt)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Checklist:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            var draggedItemId by remember { mutableStateOf<String?>(null) }
            var dragOffset by remember { mutableStateOf(0f) }
            checklistItems.forEachIndexed { _, item ->
                key(item.id) {
                    val isDragging = draggedItemId == item.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragOffset
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    shadowElevation = 8f
                                }
                            }
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedItemId = item.id
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val id = draggedItemId ?: return@detectDragGesturesAfterLongPress
                                        val currentPos = checklistItems.indexOfFirst { it.id == id }
                                        if (currentPos < 0) return@detectDragGesturesAfterLongPress
                                        val itemHeight = size.height.toFloat()
                                        if (dragOffset < -itemHeight / 2 && currentPos > 0) {
                                            val list = checklistItems.toMutableList()
                                            list.add(currentPos - 1, list.removeAt(currentPos))
                                            checklistItems = list
                                            dragOffset += itemHeight
                                        }
                                        if (dragOffset > itemHeight / 2 && currentPos < checklistItems.lastIndex) {
                                            val list = checklistItems.toMutableList()
                                            list.add(currentPos + 1, list.removeAt(currentPos))
                                            checklistItems = list
                                            dragOffset -= itemHeight
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemId = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemId = null
                                        dragOffset = 0f
                                    }
                                )
                            }
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = {
                                checklistItems = checklistItems.map { i ->
                                    if (i.id == item.id) i.copy(checked = !i.checked) else i
                                }
                            }
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            checklistItems = checklistItems.filterNot { it.id == item.id }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove item")
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newChecklistText,
                    onValueChange = { newChecklistText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("New item") },
                    singleLine = true
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = {
                    if (newChecklistText.isNotBlank()) {
                        checklistItems = checklistItems + ChecklistItem(
                            id = UUID.randomUUID().toString(),
                            text = newChecklistText.trim()
                        )
                        newChecklistText = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = { saveCard() }) {
                    Text(saveLabel)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDone) {
                    Text(cancelLabel)
                }
                if (editing != null) {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        viewModel.deleteCard(editing!!.id)
                        onDone()
                    }) {
                        Text(deleteLabel)
                    }
                }
            }
        }
    }

    if (showCreateTagDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = { Text("Create Tag") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Color:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tagPalette.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { newTagColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (newTagColorIndex == index) {
                                    Text("✓", color = androidx.compose.ui.graphics.Color.White, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank()) {
                        val id = viewModel.addTag(newTagName.trim(), newTagColorIndex)
                        cardTagIds = cardTagIds + id
                        showCreateTagDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showStartDatePicker) {
        val state = rememberDatePickerState(parseDateToMillis(datePart(taskSetTimeStart)))
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = DateHelper.millisToDate(millis)
                        val t = timePart(taskSetTimeStart)
                        taskSetTimeStart = if (t.isBlank()) date else "$date${'T'}$t"
                    }
                    showStartDatePicker = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(cancelLabel)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showStartTimePicker) {
        val currentHour = parseHour(taskSetTimeStart)
        val currentMinute = parseMinute(taskSetTimeStart)
        val state = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text(selectTimeLabel) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val d = datePart(taskSetTimeStart).ifBlank { DateHelper.todayDate() }
                    val time = "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"
                    taskSetTimeStart = "$d${'T'}$time"
                    showStartTimePicker = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(parseDateToMillis(datePart(taskSetTimeEnd)))
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = DateHelper.millisToDate(millis)
                        val t = timePart(taskSetTimeEnd)
                        taskSetTimeEnd = if (t.isBlank()) date else "$date${'T'}$t"
                    }
                    showEndDatePicker = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(cancelLabel)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndTimePicker) {
        val currentHour = parseHour(taskSetTimeEnd)
        val currentMinute = parseMinute(taskSetTimeEnd)
        val state = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text(selectTimeLabel) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val d = datePart(taskSetTimeEnd).ifBlank { DateHelper.todayDate() }
                    val time = "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"
                    taskSetTimeEnd = "$d${'T'}$time"
                    showEndTimePicker = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    if (showReminderPicker) {
        AlertDialog(
            onDismissRequest = { showReminderPicker = false },
            title = { Text(selectReminderLabel) },
            text = {
                Column {
                    listOf(
                        null to reminderNoneLabel,
                        0 to reminderAtTimeLabel,
                        5 to reminder5minLabel,
                        15 to reminder15minLabel,
                        30 to reminder30minLabel,
                        60 to reminder1hrLabel,
                        120 to reminder2hrLabel,
                        1440 to reminder1dayLabel
                    ).forEach { (value, label) ->
                        TextButton(
                            onClick = {
                                cardReminders = cardReminders + CardReminder(minutesBefore = value)
                                showReminderPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(
                        onClick = {
                            showReminderPicker = false
                            tempCustomReminderDate = ""
                            showCustomReminderDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(reminderCustomLabel, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderPicker = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    if (showCustomReminderDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = try {
                DateHelper.dateToMillis(tempCustomReminderDate.ifBlank { DateHelper.todayDate() })
            } catch (_: Exception) { DateHelper.nowCal().timeInMillis }
        )
        DatePickerDialog(
            onDismissRequest = { showCustomReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        tempCustomReminderDate = DateHelper.millisToDate(millis)
                    }
                    showCustomReminderDatePicker = false
                    showCustomReminderTimePicker = true
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderDatePicker = false }) {
                    Text(cancelLabel)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showCustomReminderTimePicker) {
        val state = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showCustomReminderTimePicker = false },
            title = { Text(reminderCustomTimeLabel) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val time = "${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"
                    val date = tempCustomReminderDate.ifBlank { DateHelper.todayDate() }
                    cardReminders = cardReminders + CardReminder(customTime = "$date${'T'}$time")
                    tempCustomReminderDate = ""
                    showCustomReminderTimePicker = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderTimePicker = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }


    if (showCustomRepeatDialog) {
        val initialSkipDates = remember(repeatSkipDates) {
            decodeSkipDates(repeatSkipDates)
        }
        var tempFrequency by remember { mutableStateOf(repeatCustomFrequency) }
        var tempSelectedDays by remember {
            mutableStateOf(repeatDaysOfWeek ?: emptyList<Int>())
        }
        var tempDayOfMonth by remember { mutableStateOf(repeatDayOfMonth ?: 1) }
        var tempMonth by remember { mutableStateOf(repeatMonth ?: 1) }
        var tempSkipDates by remember { mutableStateOf(initialSkipDates) }
        var tempEndDate by remember { mutableStateOf(repeatEndDate) }
        var showEndDatePickerForCustom by remember { mutableStateOf(false) }
        var showSkipDatePickerForCustom by remember { mutableStateOf(false) }

        val frequencies = listOf("weekly" to repeatWeeklyLabel, "biweekly" to "Every 2 weeks", "monthly" to repeatMonthlyLabel, "yearly" to repeatYearlyLabel)

        AlertDialog(
            onDismissRequest = { showCustomRepeatDialog = false },
            title = { Text(customRepeatTitleLabel) },
            text = {
                Column {
                    Text("Frequency:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        frequencies.forEach { (value, label) ->
                            FilterChip(
                                selected = tempFrequency == value,
                                onClick = { tempFrequency = value },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    if (tempFrequency == "weekly" || tempFrequency == "biweekly") {
                        Text(repeatOnDaysLabel, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        val dayEntries = listOf(
                            Calendar.MONDAY to dayMonLabel,
                            Calendar.TUESDAY to dayTueLabel,
                            Calendar.WEDNESDAY to dayWedLabel,
                            Calendar.THURSDAY to dayThuLabel,
                            Calendar.FRIDAY to dayFriLabel,
                            Calendar.SATURDAY to daySatLabel,
                            Calendar.SUNDAY to daySunLabel
                        )
                        dayEntries.forEach { (dayValue, dayLabel) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = dayValue in tempSelectedDays,
                                    onCheckedChange = { checked ->
                                        tempSelectedDays = if (checked) {
                                            tempSelectedDays + dayValue
                                        } else {
                                            tempSelectedDays - dayValue
                                        }
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(dayLabel)
                            }
                        }
                    } else if (tempFrequency == "monthly") {
                        Text("Day of month:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                tempDayOfMonth = maxOf(1, tempDayOfMonth - 1)
                            }) { Text("-") }
                            Spacer(Modifier.width(8.dp))
                            Text("$tempDayOfMonth", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                tempDayOfMonth = minOf(31, tempDayOfMonth + 1)
                            }) { Text("+") }
                        }
                    } else if (tempFrequency == "yearly") {
                        Text("Month:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        val months = listOf(
                            1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr",
                            5 to "May", 6 to "Jun", 7 to "Jul", 8 to "Aug",
                            9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            months.forEach { (m, label) ->
                                FilterChip(
                                    selected = tempMonth == m,
                                    onClick = { tempMonth = m },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Day of month:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                tempDayOfMonth = maxOf(1, tempDayOfMonth - 1)
                            }) { Text("-") }
                            Spacer(Modifier.width(8.dp))
                            Text("$tempDayOfMonth", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                tempDayOfMonth = minOf(31, tempDayOfMonth + 1)
                            }) { Text("+") }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(onClick = { showEndDatePickerForCustom = true }) {
                        Text(
                            if (tempEndDate.isNotBlank()) "$repeatUntilLabel $tempEndDate"
                            else repeatNeverEndsLabel
                        )
                    }
                    if (tempEndDate.isNotBlank()) {
                        TextButton(onClick = { tempEndDate = "" }) {
                            Text(repeatNeverEndsLabel)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(onClick = { showSkipDatePickerForCustom = true }) {
                        Text(
                            if (tempSkipDates.isEmpty()) repeatSkipDateLabel
                            else "$repeatSkipDateLabel (${tempSkipDates.size})"
                        )
                    }
                    if (tempSkipDates.isNotEmpty()) {
                        TextButton(onClick = { tempSkipDates = emptyList() }) {
                            Text("Clear")
                        }
                        Text(
                            tempSkipDates.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repeatCustomFrequency = tempFrequency
                    when (tempFrequency) {
                        "monthly" -> {
                            repeatDayOfMonth = tempDayOfMonth
                            repeatDaysOfWeek = null
                            repeatMonth = null
                        }
                        "yearly" -> {
                            repeatDayOfMonth = tempDayOfMonth
                            repeatMonth = tempMonth
                            repeatDaysOfWeek = null
                        }
                        else -> {
                            if (tempSelectedDays.isNotEmpty()) {
                                repeatDaysOfWeek = tempSelectedDays
                            }
                            repeatDayOfMonth = null
                            repeatMonth = null
                        }
                    }
                    repeatType = RepeatType.CUSTOM
                    repeatEndDate = tempEndDate
                    repeatSkipDates = if (tempSkipDates.isEmpty()) "" else encodeSkipDates(tempSkipDates)
                    showCustomRepeatDialog = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (repeatDaysOfWeek == null || repeatDaysOfWeek!!.isEmpty()) {
                        repeatType = RepeatType.DAILY
                    }
                    showCustomRepeatDialog = false
                }) { Text(cancelLabel) }
            }
        )

        if (showEndDatePickerForCustom) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = try {
                    DateHelper.dateToMillis(tempEndDate.ifBlank { DateHelper.todayDate() })
                } catch (_: Exception) { DateHelper.nowCal().timeInMillis }
            )
            DatePickerDialog(
                onDismissRequest = { showEndDatePickerForCustom = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            tempEndDate = DateHelper.millisToDate(millis)
                        }
                        showEndDatePickerForCustom = false
                    }) { Text(okLabel) }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePickerForCustom = false }) {
                        Text(cancelLabel)
                    }
                }
            ) {
                DatePicker(state = state)
            }
        }

        if (showSkipDatePickerForCustom) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = DateHelper.nowCal().timeInMillis
            )
            DatePickerDialog(
                onDismissRequest = { showSkipDatePickerForCustom = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val date = DateHelper.millisToDate(millis)
                            if (date !in tempSkipDates) {
                                tempSkipDates = tempSkipDates + date
                            }
                        }
                        showSkipDatePickerForCustom = false
                    }) { Text(okLabel) }
                },
                dismissButton = {
                    TextButton(onClick = { showSkipDatePickerForCustom = false }) {
                        Text(cancelLabel)
                    }
                }
            ) {
                DatePicker(state = state)
            }
        }
    }
}

private fun encodeSkipDates(dates: List<String>): String =
    dates.joinToString(",", "[", "]") { "\"$it\"" }

private fun decodeSkipDates(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val trimmed = json.trim('[', ']')
        if (trimmed.isBlank()) return emptyList()
        trimmed.split(",").map { it.trim().removeSurrounding("\"") }
    } catch (_: Exception) { emptyList() }
}
