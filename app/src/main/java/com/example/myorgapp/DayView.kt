package com.example.myorgapp

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi

@Composable
fun DayView(
    date: String,
    tasks: List<CardItem>,
    onEditTask: (CardItem) -> Unit,
    onToggleFinished: (CardItem) -> Unit
) {
    val timedTasks = tasks
        .filter { it.taskSetTimeStart != null }
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
                AllDayTaskCard(task = task, onToggleFinished = { onToggleFinished(task)})      }
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

@OptIn(ExperimentalFoundationApi::class)
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
            .combinedClickable(
                onClick = onEdit,
                onDoubleClick = onToggleFinished
            ),
        colors = if (task.finished) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.finished) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllDayTaskCard(
    task: CardItem,
    onToggleFinished: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = {},
                onDoubleClick = onToggleFinished
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (task.finished) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
