package com.example.myorgapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.myorgapp.RepeatType
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

data class OverdueInfo(
    val type: String,
    val hoursOrDays: Int,
    val minutes: Int
)

private fun getOverdueInfo(card: CardItem): OverdueInfo? {
    if (card.finished || card.taskSetTimeEnd == null) return null
    return try {
        val endCal = DateHelper.parseDateTime(card.taskSetTimeEnd)
        val nowCal = DateHelper.nowCal()
        if (nowCal.before(endCal)) return null

        val endDate = DateHelper.formatDate(endCal)
        val today = DateHelper.todayDate()
        val endCalDateOnly = DateHelper.parseDate(endDate)
        val todayCal = DateHelper.parseDate(today)
        val daysOverdue = DateHelper.daysBetween(endCalDateOnly, todayCal).toInt()

        if (daysOverdue > 0) {
            return OverdueInfo("days", daysOverdue, 0)
        }
        val minutesOverdue = DateHelper.minutesBetween(endCal, nowCal).toInt()
        val hours = minutesOverdue / 60
        val minutes = minutesOverdue % 60
        OverdueInfo("time", hours, minutes)
    } catch (_: Exception) { null }
}

data class TimeWindowInfo(
    val dayType: String,
    val dayCount: Int,
    val timePart: String
)

private fun getTimeWindowInfo(start: String?, end: String?): TimeWindowInfo? {
    if (start.isNullOrBlank()) return null
    return try {
        val startCal = DateHelper.parseDateTime(start)
        val startDate = DateHelper.formatDate(startCal)
        val today = DateHelper.todayDate()

        val dayType: String = when {
            startDate == today -> "today"
            startDate == DateHelper.addDays(today, 1) -> "tomorrow"
            startDate > today -> "future"
            else -> "past"
        }
        val dayCount = when {
            startDate == today || startDate == DateHelper.addDays(today, 1) -> 0
            startDate > today -> DateHelper.daysBetween(DateHelper.parseDate(today), startCal).toInt()
            else -> DateHelper.daysBetween(startCal, DateHelper.parseDate(today)).toInt()
        }

        val startTime = DateHelper.formatTime(startCal)
        val endStr = if (!end.isNullOrBlank()) {
            try {
                val endCal = DateHelper.parseDateTime(end)
                " - ${DateHelper.formatTime(endCal)}"
            } catch (_: Exception) { "" }
        } else { "" }

        TimeWindowInfo(dayType, dayCount, "$startTime$endStr")
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SharedCardViewModel,
    onAdd: () -> Unit,
    onEdit: (CardItem) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleFinished: (CardItem) -> Unit,
    onDeleteCompleted: (Long) -> Unit,
    onSettings: () -> Unit,
    onCalendar: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val completedCards by viewModel.completedCards.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val tasksLabel = stringResource(R.string.tasks)
    val activeLabel = stringResource(R.string.active)
    val completedTabLabel = stringResource(R.string.completed_tab)
    val noActiveCardsLabel = stringResource(R.string.no_active_cards)
    val noCompletedCardsLabel = stringResource(R.string.no_completed_cards)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tasksLabel) },
                actions = {
                    IconButton(onClick = onCalendar) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.content_desc_calendar)
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.content_desc_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onAdd) { Text("+") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(activeLabel) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(completedTabLabel) }
                )
            }
            when (selectedTab) {
                0 -> {
                    val today = DateHelper.todayDate()
                    val sortedCards = cards.sortedWith(compareBy<CardItem> { card ->
                        when {
                            card.finished -> 4L
                            card.taskSetTimeStart == null -> 3L
                            DateHelper.getDatePart(card.taskSetTimeStart) < today -> 0L
                            DateHelper.getDatePart(card.taskSetTimeStart) == today -> 1L
                            else -> 2L
                        }
                    }.thenBy { it.taskSetTimeStart })

                    if (cards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(noActiveCardsLabel)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(sortedCards) { card ->
                                val overdueInfo = getOverdueInfo(card)
                                ActiveCardRow(
                                    card = card,
                                    overdueInfo = overdueInfo,
                                    onClick = { onEdit(card) },
                                    onToggleFinished = { onToggleFinished(card) },
                                    onDelete = { onDelete(card.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (completedCards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(noCompletedCardsLabel)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(completedCards) { card ->
                                CompletedCardRow(
                                    card = card,
                                    onDelete = { onDeleteCompleted(card.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveCardRow(
    card: CardItem,
    overdueInfo: OverdueInfo?,
    onClick: () -> Unit,
    onToggleFinished: () -> Unit,
    onDelete: () -> Unit
) {
    val timeInfo = getTimeWindowInfo(card.taskSetTimeStart, card.taskSetTimeEnd)

    val overdueText: String? = overdueInfo?.let { info ->
        when (info.type) {
            "days" -> stringResource(R.string.overdue_days, info.hoursOrDays)
            "time" -> if (info.hoursOrDays > 0) {
                stringResource(R.string.overdue_time, info.hoursOrDays, info.minutes)
            } else {
                stringResource(R.string.overdue_time_mins, info.minutes)
            }
            else -> null
        }
    }

    val timeWindowText: String? = timeInfo?.let { info ->
        val dayLabel = when (info.dayType) {
            "today" -> stringResource(R.string.today)
            "tomorrow" -> stringResource(R.string.tomorrow)
            "future" -> stringResource(R.string.in_x_days, info.dayCount)
            "past" -> stringResource(R.string.x_days_ago, info.dayCount)
            else -> ""
        }
        "$dayLabel  ${info.timePart}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onToggleFinished
            ),
        colors = if (overdueText != null) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (overdueText != null) {
                        Text(
                            text = "($overdueText) ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (overdueText != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (card.finished) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.content_desc_finished),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = card.description, style = MaterialTheme.typography.bodyMedium)
                val hasReminder = card.reminderMinutesBefore != null || card.reminderCustomTime != null
                if (timeWindowText != null || hasReminder || card.repeatType != RepeatType.NONE) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasReminder) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        if (card.repeatType != RepeatType.NONE) {
                            Icon(
                                imageVector = Icons.Filled.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        if (timeWindowText != null) {
                            Text(
                                text = timeWindowText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_desc_delete)
                )
            }
        }
    }
}

@Composable
private fun CompletedCardRow(
    card: CardItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_completed),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Text(text = card.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(text = card.description, style = MaterialTheme.typography.bodyMedium)
                if (card.dateCompleted != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.completed_on, card.dateCompleted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_desc_delete)
                )
            }
        }
    }
}
