package com.example.myorgapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun MonthView(
    firstOfMonth: String,
    tasks: List<CardItem>,
    onDaySelected: (String) -> Unit
) {
    val today = DateHelper.todayDate()
    val startDayOfWeek = DateHelper.getDayOfWeekMondayBased(firstOfMonth)
    val totalDays = DateHelper.getDaysInMonth(firstOfMonth)
    val totalCells = startDayOfWeek + totalDays
    val rows = (totalCells + 6) / 7

    val taskMap = mutableMapOf<String, List<CardItem>>()
    tasks.forEach { task ->
        task.taskSetTimeStart?.let {
            try {
                val date = DateHelper.getDatePart(it)
                taskMap[date] = (taskMap[date] ?: emptyList()) + task
            } catch (_: Exception) {}
        }
    }

    val dayHeaders = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Row {
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startDayOfWeek + 1

                    if (dayNumber in 1..totalDays) {
                        val dayStr = firstOfMonth.substring(0, 7) + "-%02d".format(dayNumber)
                        MonthDayCell(
                            date = dayStr,
                            dayTasks = taskMap[dayStr] ?: emptyList(),
                            isToday = dayStr == today,
                            onClick = { onDaySelected(dayStr) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: String,
    dayTasks: List<CardItem>,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNumber = DateHelper.getDayOfMonth(date)

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isToday) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = dayNumber.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(2.dp)
                )
            }
        } else {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (dayTasks.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                dayTasks.take(3).forEachIndexed { _, task ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .padding(horizontal = 1.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(5.dp),
                            shape = CircleShape,
                            color = if (task.finished) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        ) {}
                    }
                }
                if (dayTasks.size > 3) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
        }
    }
}
