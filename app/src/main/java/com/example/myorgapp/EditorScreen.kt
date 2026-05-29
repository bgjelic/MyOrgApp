package com.example.myorgapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import java.util.Calendar
import java.util.UUID
import androidx.compose.foundation.rememberScrollState
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
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var nameHasError by remember { mutableStateOf(false) }
    var taskSetTimeStart by remember { mutableStateOf(editing?.taskSetTimeStart ?: "") }
    var taskSetTimeEnd by remember { mutableStateOf(editing?.taskSetTimeEnd ?: "") }
    var reminderMinutesBefore by remember { mutableStateOf(editing?.reminderMinutesBefore) }
    var reminderCustomTime by remember { mutableStateOf(editing?.reminderCustomTime) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showCustomReminderDatePicker by remember { mutableStateOf(false) }
    var showCustomReminderTimePicker by remember { mutableStateOf(false) }
    var tempCustomReminderDate by remember { mutableStateOf("") }

    var repeatType by remember { mutableStateOf(editing?.repeatType ?: RepeatType.NONE) }
    var repeatDaysOfWeek by remember { mutableStateOf(editing?.repeatDaysOfWeek) }
    var repeatEndDate by remember { mutableStateOf(editing?.repeatEndDate ?: "") }
    var repeatSkipDates by remember { mutableStateOf(editing?.repeatSkipDates ?: "") }

    var showRepeatPicker by remember { mutableStateOf(false) }
    var showCustomRepeatDialog by remember { mutableStateOf(false) }

    var checklistItems by remember { mutableStateOf(editing?.checklist ?: emptyList()) }
    var newChecklistText by remember { mutableStateOf("") }

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
    val repeatWeekdaysLabel = stringResource(R.string.repeat_weekdays)
    val repeatWeekendsLabel = stringResource(R.string.repeat_weekends)
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
        name = editing?.name ?: ""
        description = editing?.description ?: ""
        taskSetTimeStart = editing?.taskSetTimeStart ?: ""
        taskSetTimeEnd = editing?.taskSetTimeEnd ?: ""
        reminderMinutesBefore = editing?.reminderMinutesBefore
        reminderCustomTime = editing?.reminderCustomTime
        repeatType = editing?.repeatType ?: RepeatType.NONE
        repeatDaysOfWeek = editing?.repeatDaysOfWeek
        repeatEndDate = editing?.repeatEndDate ?: ""
        repeatSkipDates = editing?.repeatSkipDates ?: ""
        checklistItems = editing?.checklist ?: emptyList()
        newChecklistText = ""
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameHasError = false },
                label = { Text(nameLabel) },
                isError = nameHasError,
                supportingText = { if (nameHasError) Text(nameRequiredLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions =KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
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
            OutlinedButton(onClick = { showReminderPicker = true }) {
                val customTime = reminderCustomTime
                val reminderText = if (customTime != null) {
                    "Custom: ${customTime.replace("T", " ")}"
                } else when (reminderMinutesBefore) {
                    null -> reminderNoneLabel
                    0 -> reminderAtTimeLabel
                    5 -> reminder5minLabel
                    15 -> reminder15minLabel
                    30 -> reminder30minLabel
                    60 -> reminder1hrLabel
                    120 -> reminder2hrLabel
                    1440 -> reminder1dayLabel
                    else -> "$reminderMinutesBefore min"
                }
                Text(reminderText)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "$repeatLabel:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { showRepeatPicker = true }) {
                val text = if (repeatType == RepeatType.CUSTOM) {
                    val dayCount = repeatDaysOfWeek?.size ?: 0
                    if (dayCount > 0) "$repeatCustomLabel ($dayCount)"
                    else repeatCustomLabel
                } else when (repeatType) {
                    RepeatType.NONE -> repeatNoneLabel
                    RepeatType.DAILY -> repeatDailyLabel
                    RepeatType.WEEKDAYS -> repeatWeekdaysLabel
                    RepeatType.WEEKENDS -> repeatWeekendsLabel
                    RepeatType.WEEKLY -> repeatWeeklyLabel
                    RepeatType.MONTHLY -> repeatMonthlyLabel
                    RepeatType.YEARLY -> repeatYearlyLabel
                    else -> repeatNoneLabel
                }
                Text(text)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Checklist:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            checklistItems.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                Button(onClick = {
                    if (name.isBlank()) {
                        nameHasError = true
                        return@Button
                    }
                    if (editing == null) {
                        viewModel.addCard(
                            name = name.trim(),
                            description = description.trim(),
                            taskSetTimeStart = taskSetTimeStart.ifBlank { null },
                            taskSetTimeEnd = taskSetTimeEnd.ifBlank { null },
                            reminderMinutesBefore = reminderMinutesBefore,
                            reminderCustomTime = reminderCustomTime,
                            repeatType = repeatType,
                            repeatDaysOfWeek = repeatDaysOfWeek,
                            repeatEndDate = repeatEndDate.ifBlank { null },
                            repeatSkipDates = repeatSkipDates.ifBlank { null },
                            checklist = checklistItems
                        )
                    } else {
                        val current = editing!!
                        val updated = current.copy(
                            name = name.trim(),
                            description = description.trim(),
                            taskSetTimeStart = taskSetTimeStart.ifBlank { null },
                            taskSetTimeEnd = taskSetTimeEnd.ifBlank { null },
                            reminderMinutesBefore = reminderMinutesBefore,
                            reminderCustomTime = reminderCustomTime,
                            repeatType = repeatType,
                            repeatDaysOfWeek = repeatDaysOfWeek,
                            repeatEndDate = repeatEndDate.ifBlank { null },
                            repeatSkipDates = repeatSkipDates.ifBlank { null },
                            checklist = checklistItems
                        )
                        viewModel.updateCard(updated)
                    }
                    onDone()
                }) {
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
                                reminderMinutesBefore = value
                                reminderCustomTime = null
                                showReminderPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(
                        onClick = {
                            reminderMinutesBefore = null
                            reminderCustomTime = null
                            showReminderPicker = false
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
                    reminderCustomTime = "$date${'T'}$time"
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

    if (showRepeatPicker) {
        AlertDialog(
            onDismissRequest = { showRepeatPicker = false },
            title = { Text(selectRepeatLabel) },
            text = {
                Column {
                    listOf(
                        RepeatType.NONE to repeatNoneLabel,
                        RepeatType.DAILY to repeatDailyLabel,
                        RepeatType.WEEKDAYS to repeatWeekdaysLabel,
                        RepeatType.WEEKENDS to repeatWeekendsLabel,
                        RepeatType.WEEKLY to repeatWeeklyLabel,
                        RepeatType.MONTHLY to repeatMonthlyLabel,
                        RepeatType.YEARLY to repeatYearlyLabel
                    ).forEach { (value, label) ->
                        TextButton(
                            onClick = {
                                repeatType = value
                                if (value != RepeatType.CUSTOM) {
                                    showRepeatPicker = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(
                        onClick = {
                            repeatType = RepeatType.CUSTOM
                            showRepeatPicker = false
                            showCustomRepeatDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(repeatCustomLabel, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatPicker = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    if (showCustomRepeatDialog) {
        var selectedDays by remember {
            mutableStateOf(repeatDaysOfWeek ?: emptyList<Int>())
        }
        val initialSkipDates = remember(repeatSkipDates) {
            decodeSkipDates(repeatSkipDates)
        }
        var tempSkipDates by remember { mutableStateOf(initialSkipDates) }
        var tempEndDate by remember { mutableStateOf(repeatEndDate) }
        var showEndDatePickerForCustom by remember { mutableStateOf(false) }
        var showSkipDatePickerForCustom by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomRepeatDialog = false },
            title = { Text(customRepeatTitleLabel) },
            text = {
                Column {
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
                                checked = dayValue in selectedDays,
                                onCheckedChange = { checked ->
                                    selectedDays = if (checked) {
                                        selectedDays + dayValue
                                    } else {
                                        selectedDays - dayValue
                                    }
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(dayLabel)
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
                    if (selectedDays.isNotEmpty()) {
                        repeatDaysOfWeek = selectedDays
                        repeatType = RepeatType.CUSTOM
                        repeatEndDate = tempEndDate
                        repeatSkipDates = if (tempSkipDates.isEmpty()) "" else encodeSkipDates(tempSkipDates)
                    }
                    showCustomRepeatDialog = false
                }) { Text(okLabel) }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (repeatDaysOfWeek == null || repeatDaysOfWeek!!.isEmpty()) {
                        repeatType = RepeatType.NONE
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
