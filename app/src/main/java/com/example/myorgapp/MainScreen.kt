package com.example.myorgapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.myorgapp.RepeatType
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

data class OverdueInfo(
    val type: String,
    val hoursOrDays: Int,
    val minutes: Int
)

private fun getOverdueInfo(card: CardItem): OverdueInfo? {
    if (card.finished || card.taskSetTimeEnd == null) return null
    if (card.repeatType != RepeatType.NONE) return null
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
    val tags by viewModel.tags.collectAsState()
    val tagFilter by viewModel.activeTagFilter.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val cardOrder by viewModel.cardOrder.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = remember(cards, tagFilter, searchQuery) {
        cards
            .filter { card -> tagFilter == null || card.tagIds.contains(tagFilter) }
            .filter { card ->
                searchQuery.isBlank() ||
                card.name.contains(searchQuery, ignoreCase = true) ||
                card.description.contains(searchQuery, ignoreCase = true) ||
                card.checklist.any { it.text.contains(searchQuery, ignoreCase = true) }
            }
    }

    val tasksLabel = stringResource(R.string.tasks)
    val activeLabel = stringResource(R.string.active)
    val completedTabLabel = stringResource(R.string.completed_tab)
    val noActiveCardsLabel = stringResource(R.string.no_active_cards)
    val noCompletedCardsLabel = stringResource(R.string.no_completed_cards)

    val highlightedCardId by viewModel.highlightedCardId.collectAsState()
    LaunchedEffect(highlightedCardId) {
        if (highlightedCardId != null) {
            delay(2000)
            viewModel.setHighlightedCardId(null)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(toastMessage!!)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    val todayCards = remember(cards) { viewModel.getCardsForDate(today) }
                    val todayCompletedFromCompleted = remember(completedCards) {
                        completedCards.filter { it.dateCompleted == today }
                    }
                    val totalToday = todayCards.size + todayCompletedFromCompleted.size
                    val completedToday = todayCards.count { it.finished } + todayCompletedFromCompleted.size
                    val sortedCards = remember(filteredCards, sortMode, cardOrder) {
                        if (sortMode == "custom") {
                            val orderMap = cardOrder.withIndex().associate { (idx, id) -> id to idx }
                            filteredCards.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
                        } else {
                            filteredCards.sortedWith(compareBy<CardItem> { card ->
                                when {
                                    card.finished -> 4L
                                    card.taskSetTimeStart == null -> 3L
                                    DateHelper.getDatePart(card.taskSetTimeStart) < today -> 0L
                                    DateHelper.getDatePart(card.taskSetTimeStart) == today -> 1L
                                    else -> 2L
                                }
                            }.thenBy { it.taskSetTimeStart })
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        placeholder = { Text("Search cards...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    )

                    if (tags.isNotEmpty()) {
                        val tagScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .heightIn(max = 112.dp)
                                .verticalScroll(tagScroll)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = tagFilter == null,
                                    onClick = { viewModel.setTagFilter(null) },
                                    label = { Text("All") }
                                )
                                tags.forEach { tag ->
                                    FilterChip(
                                        selected = tagFilter == tag.id,
                                        onClick = { viewModel.setTagFilter(tag.id) },
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
                    }

                    if (filteredCards.isEmpty()) {
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
                            if (totalToday > 0) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Today: $completedToday/$totalToday done",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (streak > 0) {
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = "🔥 $streak days",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                viewModel.setSortMode(if (sortMode == "auto") "custom" else "auto")
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = if (sortMode == "auto") "Switch to custom order" else "Switch to auto sort",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (sortMode == "custom") MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            items(sortedCards) { card ->
                                val overdueInfo = remember(card) { getOverdueInfo(card) }
                                ActiveCardRow(
                                    card = card,
                                    tags = tags,
                                    overdueInfo = overdueInfo,
                                    highlightedCardId = highlightedCardId,
                                    onClick = {
                                        viewModel.setHighlightedCardId(null)
                                        onEdit(card)
                                    },
                                    onToggleFinished = { onToggleFinished(card) },
                                    onDelete = { onDelete(card.id) },
                                    onToggleChecklistItem = { itemId -> viewModel.toggleChecklistItem(card.id, itemId) },
                                    onMoveUp = { viewModel.moveCardUp(card.id) },
                                    onMoveDown = { viewModel.moveCardDown(card.id) },
                                    showReorderButtons = sortMode == "custom"
                                )
                            }
                        }
                    }
                }
                 1 -> {
                    val completedRepeats = remember(cards) { cards.filter { it.repeatType != RepeatType.NONE && it.repeatCompletionCount > 0 } }
                    val hasRegular = completedCards.isNotEmpty()
                    val hasRepeats = completedRepeats.isNotEmpty()

                    if (!hasRegular && !hasRepeats) {
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
                            if (hasRegular) {
                                item {
                                    Text(
                                        text = stringResource(R.string.completed_tab),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                    Text(
                                        text = "(click to restore)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                                items(completedCards) { card ->
                                    CompletedCardRow(
                                        card = card,
                                        onToggle = { onToggleFinished(card) },
                                        onDelete = { onDeleteCompleted(card.id) }
                                    )
                                }
                            }
                            if (hasRepeats) {
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Completed Repeats",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                                items(completedRepeats) { card ->
                                    CompletedRepeatRow(card = card)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val showYesterdayDialog by viewModel.showYesterdayDialog.collectAsState()
    val yesterdayCards by viewModel.yesterdayUncompleted.collectAsState()

    if (showYesterdayDialog && yesterdayCards.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissYesterdayDialog() },
            title = {
                Text("Did you forget to check off any of these?")
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    yesterdayCards.forEach { card ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.checkYesterdayCard(card) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = { viewModel.checkYesterdayCard(card) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissYesterdayDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ActiveCardRow(
    card: CardItem,
    tags: List<CardTag> = emptyList(),
    overdueInfo: OverdueInfo?,
    highlightedCardId: Long? = null,
    onClick: () -> Unit,
    onToggleFinished: () -> Unit,
    onDelete: () -> Unit,
    onToggleChecklistItem: (String) -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    showReorderButtons: Boolean = false
) {
    val timeInfo = remember(card) { getTimeWindowInfo(card.taskSetTimeStart, card.taskSetTimeEnd) }

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

    val checkExpanded = remember { mutableStateOf(false) }
    val isHighlighted = highlightedCardId == card.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .then(
                if (isHighlighted) Modifier.border(
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        colors = when {
            isHighlighted -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            card.finished -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            overdueText != null -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            else -> CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(onClick = onToggleFinished) {
                Icon(
                    imageVector = if (card.finished) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = stringResource(R.string.content_desc_finished),
                    tint = if (card.finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                    if (card.checklist.isNotEmpty()) {
                        val checked = card.checklist.count { it.checked }
                        val total = card.checklist.size
                        Text(
                            text = "($checked/$total)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val matchedTags = tags.filter { it.id in card.tagIds }
                    matchedTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(tagPalette[tag.colorIndex % tagPalette.size].copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = tagPalette[tag.colorIndex % tagPalette.size]
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = card.description, style = MaterialTheme.typography.bodyMedium)
                val hasReminder = card.reminders.isNotEmpty()
                if (timeWindowText != null || hasReminder || card.repeatType != RepeatType.NONE || card.checklist.isNotEmpty()) {
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
                            if (card.repeatCompletionCount > 0) {
                                val unit = when (card.repeatType) {
                                    RepeatType.DAILY, RepeatType.WEEKDAYS, RepeatType.WEEKENDS, RepeatType.CUSTOM -> "days"
                                    RepeatType.WEEKLY -> "weeks"
                                    RepeatType.MONTHLY -> "months"
                                    RepeatType.YEARLY -> "years"
                                    else -> ""
                                }
                                Text(
                                    text = "×${card.repeatCompletionCount} $unit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (timeWindowText != null) {
                            Text(
                                text = timeWindowText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (card.checklist.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { checkExpanded.value = !checkExpanded.value },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = if (checkExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                }
                AnimatedVisibility(visible = checkExpanded.value) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    ) {
                        card.checklist.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { onToggleChecklistItem(item.id) },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            if (showReorderButtons) {
                Column {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
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
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onToggle() },
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

@Composable
private fun CompletedRepeatRow(card: CardItem) {
    val unit = when (card.repeatType) {
        RepeatType.DAILY, RepeatType.WEEKDAYS, RepeatType.WEEKENDS, RepeatType.CUSTOM -> "days"
        RepeatType.WEEKLY -> "weeks"
        RepeatType.MONTHLY -> "months"
        RepeatType.YEARLY -> "years"
        else -> ""
    }
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
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
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
                Text(
                    text = "×${card.repeatCompletionCount} $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
