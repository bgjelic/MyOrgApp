package com.example.myorgapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WeekView(
    startOfWeek: String,
    tasks: List<CardItem>,
    onEditTask: (CardItem) -> Unit,
    onToggleFinished: (CardItem) -> Unit
) {
    val days = (0..6).map { DateHelper.addDays(startOfWeek, it) }
    val today = DateHelper.todayDate()

    val hScroll = rememberScrollState()
    Row(modifier = Modifier.horizontalScroll(hScroll)) {
        days.forEach { day ->
            val dayTasks = tasks.filter { task ->
                task.taskSetTimeStart?.let {
                    try {
                        DateHelper.getDatePart(it) == day
                    } catch (_: Exception) { false }
                } ?: false
            }
            WeekDayColumn(
                day = day,
                tasks = dayTasks,
                isToday = day == today,
                onEditTask = onEditTask,
                onToggleFinished = onToggleFinished
            )
        }
    }
}

@Composable
private fun WeekDayColumn(
    day: String,
    tasks: List<CardItem>,
    isToday: Boolean,
    onEditTask: (CardItem) -> Unit,
    onToggleFinished: (CardItem) -> Unit
) {
    val dayName = DateHelper.getDayName(day)
    val dayNumber = DateHelper.getDayOfMonth(day)

    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = dayNumber.toString(),
            style = if (isToday) MaterialTheme.typography.titleSmall
                    else MaterialTheme.typography.bodyMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        if (isToday) {
            Surface(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.CenterHorizontally),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary
            ) {}
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()

        if (tasks.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        } else {
            tasks.forEach { task ->
                WeekTaskCard(
                    task = task,
                    onEdit = { onEditTask(task) },
                    onToggleFinished = { onToggleFinished(task) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun WeekTaskCard(
    task: CardItem,
    onEdit: () -> Unit,
    onToggleFinished: () -> Unit
) {
    val timeStr = task.taskSetTimeStart?.let {
        try {
            DateHelper.formatTime(DateHelper.parseDateTime(it))
        } catch (_: Exception) { null }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = if (task.finished) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFinished, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (task.finished) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (task.finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.padding(start = 2.dp)) {
                if (timeStr != null) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
