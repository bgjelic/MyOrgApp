package com.example.myorgapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: SharedCardViewModel,
    onBack: () -> Unit,
    onDayDrill: (CardItem) -> Unit,
    onToggleFinished: (CardItem) -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    var viewMode by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(DateHelper.todayDate()) }

    val title = when (viewMode) {
        0 -> DateHelper.formatDayHeader(selectedDate)
        1 -> {
            val startOfWeek = DateHelper.getStartOfWeek(selectedDate)
            val endOfWeek = DateHelper.addDays(startOfWeek, 6)
            "${DateHelper.formatDateShort(startOfWeek)} - ${DateHelper.formatDateShort(endOfWeek)}"
        }
        2 -> DateHelper.formatMonthHeader(selectedDate)
        else -> ""
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedDate = when (viewMode) {
                            0 -> DateHelper.addDays(selectedDate, -1)
                            1 -> DateHelper.addWeeks(selectedDate, -1)
                            2 -> DateHelper.addMonths(selectedDate, -1)
                            else -> selectedDate
                        }
                    }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.content_desc_previous)
                        )
                    }
                    IconButton(onClick = {
                        selectedDate = when (viewMode) {
                            0 -> DateHelper.addDays(selectedDate, 1)
                            1 -> DateHelper.addWeeks(selectedDate, 1)
                            2 -> DateHelper.addMonths(selectedDate, 1)
                            else -> selectedDate
                        }
                    }) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.content_desc_next)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = viewMode) {
                Tab(
                    selected = viewMode == 0,
                    onClick = { viewMode = 0 },
                    text = { Text(stringResource(R.string.day)) }
                )
                Tab(
                    selected = viewMode == 1,
                    onClick = { viewMode = 1 },
                    text = { Text(stringResource(R.string.week)) }
                )
                Tab(
                    selected = viewMode == 2,
                    onClick = { viewMode = 2 },
                    text = { Text(stringResource(R.string.month)) }
                )
            }

            when (viewMode) {
                0 -> {
                    val dayTasks = viewModel.getCardsForDate(selectedDate)
                    DayView(
                        date = selectedDate,
                        tasks = dayTasks,
                        onEditTask = { card -> onDayDrill(card) },
                        onToggleFinished = { card -> onToggleFinished(card) }
                    )
                }
                1 -> {
                    val startOfWeek = DateHelper.getStartOfWeek(selectedDate)
                    val weekTasks = viewModel.getCardsForWeek(startOfWeek)
                    WeekView(
                        startOfWeek = startOfWeek,
                        tasks = weekTasks,
                        onEditTask = { card -> onDayDrill(card) },
                        onToggleFinished = { card -> onToggleFinished(card) }
                    )
                }
                2 -> {
                    val firstOfMonth = DateHelper.getFirstOfMonth(selectedDate)
                    val monthTasks = viewModel.getCardsForMonth(DateHelper.getYearMonth(selectedDate))
                    MonthView(
                        firstOfMonth = firstOfMonth,
                        tasks = monthTasks,
                        onDaySelected = { date ->
                            selectedDate = date
                            viewMode = 0
                        }
                    )
                }
            }
        }
    }
}
