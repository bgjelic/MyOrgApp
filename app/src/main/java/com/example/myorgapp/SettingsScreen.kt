package com.example.myorgapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SharedCardViewModel, onDone: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    val tags by viewModel.tags.collectAsState()
    var showTagDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<CardTag?>(null) }
    var tagDialogName by remember { mutableStateOf("") }
    var tagDialogColorIndex by remember { mutableStateOf(0) }

    val settingsTitle = stringResource(R.string.settings_title)
    val appearanceLabel = stringResource(R.string.settings_appearance)
    val calendarLabel = stringResource(R.string.settings_calendar)
    val themeLabel = stringResource(R.string.settings_theme)
    val themeSystemLabel = stringResource(R.string.settings_theme_system)
    val themeLightLabel = stringResource(R.string.settings_theme_light)
    val themeDarkLabel = stringResource(R.string.settings_theme_dark)
    val dayStartsAtLabel = stringResource(R.string.settings_day_starts_at)
    val selectTimeLabel = stringResource(R.string.settings_select_time)
    val defaultReminderTimeLabel = stringResource(R.string.settings_default_reminder_time)
    val selectReminderTimeLabel = stringResource(R.string.settings_select_reminder_time)
    val cancelLabel = stringResource(R.string.cancel)
    val okLabel = stringResource(R.string.ok)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(settingsTitle) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = appearanceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                SettingsPreferenceItem(
                    title = themeLabel,
                    subtitle = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> themeSystemLabel
                        ThemeMode.LIGHT -> themeLightLabel
                        ThemeMode.DARK -> themeDarkLabel
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = {
                            viewModel.updateSettings(settings.copy(themeMode = ThemeMode.SYSTEM))
                        },
                        label = { Text(themeSystemLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = {
                            viewModel.updateSettings(settings.copy(themeMode = ThemeMode.LIGHT))
                        },
                        label = { Text(themeLightLabel) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = {
                            viewModel.updateSettings(settings.copy(themeMode = ThemeMode.DARK))
                        },
                        label = { Text(themeDarkLabel) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = calendarLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                SettingsPreferenceItem(
                    title = dayStartsAtLabel,
                    subtitle = String.format("%02d:%02d", settings.dayStartsHour, settings.dayStartsMinute),
                    onClick = { showTimePickerDialog = true }
                )

                SettingsPreferenceItem(
                    title = defaultReminderTimeLabel,
                    subtitle = String.format("%02d:%02d", settings.defaultReminderHour, settings.defaultReminderMinute),
                    onClick = { showReminderTimePicker = true }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Manage Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                tags.forEach { tag ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagPalette[tag.colorIndex % tagPalette.size])
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        editingTag = tag
                                        tagDialogName = tag.name
                                        tagDialogColorIndex = tag.colorIndex
                                        showTagDialog = true
                                    }
                            )
                            IconButton(onClick = { viewModel.deleteTag(tag.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete tag"
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        editingTag = null
                        tagDialogName = ""
                        tagDialogColorIndex = 0
                        showTagDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Tag")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.dayStartsHour,
            initialMinute = settings.dayStartsMinute,
            is24Hour = true
        )

        Dialog(
            onDismissRequest = { showTimePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectTimeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePickerDialog = false }) {
                            Text(cancelLabel)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            viewModel.updateSettings(
                                settings.copy(
                                    dayStartsHour = timePickerState.hour,
                                    dayStartsMinute = timePickerState.minute
                                )
                            )
                            showTimePickerDialog = false
                        }) {
                            Text(okLabel)
                        }
                    }
                }
            }
        }
    }

    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text(if (editingTag != null) "Edit Tag" else "Create Tag") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tagDialogName,
                        onValueChange = { tagDialogName = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Color", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tagPalette.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { tagDialogColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (tagDialogColorIndex == index) {
                                    Text(
                                        text = "✓",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tagDialogName.isNotBlank()) {
                            if (editingTag != null) {
                                viewModel.updateTag(editingTag!!.id, tagDialogName.trim(), tagDialogColorIndex)
                            } else {
                                viewModel.addTag(tagDialogName.trim(), tagDialogColorIndex)
                            }
                        }
                        showTagDialog = false
                    }
                ) {
                    Text(okLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.defaultReminderHour,
            initialMinute = settings.defaultReminderMinute,
            is24Hour = true
        )

        Dialog(
            onDismissRequest = { showReminderTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectReminderTimeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showReminderTimePicker = false }) {
                            Text(cancelLabel)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            viewModel.updateSettings(
                                settings.copy(
                                    defaultReminderHour = timePickerState.hour,
                                    defaultReminderMinute = timePickerState.minute
                                )
                            )
                            showReminderTimePicker = false
                        }) {
                            Text(okLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPreferenceItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
