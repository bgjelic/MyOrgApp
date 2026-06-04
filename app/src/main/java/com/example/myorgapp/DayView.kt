package com.example.myorgapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun DayView(
    date: String,
    tasks: List<CardItem>,
    onEditTask: (CardItem) -> Unit,
    onToggleFinished: (CardItem) -> Unit
) {
    val timedTasks = tasks
        .filter { !it.taskSetTimeStart.isNullOrBlank() }
        .sortedBy { it.taskSetTimeStart }

    val allDayTasks = tasks.filter { it.taskSetTimeStart == null }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (allDayTasks.isNotEmpty()) {
            Text(
                stringResource(R.string.all_day),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            allDayTasks.forEach { task ->
                AllDayTaskCard(task = task, onToggleFinished = { onToggleFinished(task) }, onEdit = { onEditTask(task) })
            }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        for (hour in 0..23) {
            val hourTasks = timedTasks.filter { task ->
                DateHelper.getHour(task.taskSetTimeStart!!) == hour
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "${hour.toString().padStart(2, '0')}:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(48.dp)
                        .padding(top = 4.dp)
                )

                VerticalDivider(modifier = Modifier.heightIn(min = 60.dp))

                if (hourTasks.isEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp, top = 2.dp)) {
                        hourTasks.forEach { task ->
                            val timeRange = DateHelper.formatTimeRange(
                                task.taskSetTimeStart!!,
                                task.taskSetTimeEnd
                            )
                            HourTaskCard(
                                task = task,
                                timeRange = timeRange,
                                onEdit = { onEditTask(task) },
                                onToggleFinished = { onToggleFinished(task) }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun HourTaskCard(
    task: CardItem,
    timeRange: String,
    onEdit: () -> Unit,
    onToggleFinished: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = if (task.finished) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFinished) {
                Icon(
                    imageVector = if (task.finished) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (task.finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun AllDayTaskCard(
    task: CardItem,
    onToggleFinished: () -> Unit,
    onEdit: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFinished) {
                Icon(
                    imageVector = if (task.finished) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (task.finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
