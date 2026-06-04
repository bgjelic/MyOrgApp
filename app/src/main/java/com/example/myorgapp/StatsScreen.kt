package com.example.myorgapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: SharedCardViewModel,
    onBack: () -> Unit
) {
    val streak by viewModel.streak.collectAsState()
    viewModel.cards.collectAsState()
    viewModel.completedCards.collectAsState()
    var selectedDays by remember { mutableStateOf(14) }

    val totalCreated = viewModel.getTotalCreated()
    val totalCompleted = viewModel.getTotalCompleted()
    val completionRate = viewModel.getCompletionRate()
    val activeCount = viewModel.getActiveCount()
    val dailyData = viewModel.getDailyCompletions(selectedDays)
    val tagData = viewModel.getTagDistribution()
    val priorityData = viewModel.getPriorityDistribution()
    val overdueCount = viewModel.getOverdueCount()
    val weekdayData = viewModel.getWeekdayDistribution()
    val gridData = viewModel.getCompletionGrid(12)
    val overdueByPriority = viewModel.getOverdueByPriority()
    val tagColorMap = viewModel.tags.value.associate { it.name to it.colorIndex }

    val hasData = totalCreated > 0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SummaryRow(
                    totalCreated = totalCreated,
                    totalCompleted = totalCompleted,
                    completionRate = completionRate,
                    activeCount = activeCount
                )

                SectionTitle(stringResource(R.string.stats_daily))
                DayRangeToggle(
                    selectedDays = selectedDays,
                    onSelect = { selectedDays = it }
                )
                DailyChart(
                    data = dailyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                SectionTitle(stringResource(R.string.stats_weekday_heatmap))
                WeekdayHeatmap(data = weekdayData)

                if (tagData.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.stats_by_tag))
                    TagSection(data = tagData, tagColorMap = tagColorMap)
                }

                if (priorityData.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.stats_by_priority))
                    PrioritySection(priorityData)
                }

                SectionTitle(stringResource(R.string.stats_streak_calendar))
                StreakSection(
                    streak = streak,
                    gridData = gridData
                )

                OverdueSection(
                    overdueCount = overdueCount,
                    overdueByPriority = overdueByPriority
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    totalCreated: Int,
    totalCompleted: Int,
    completionRate: Float,
    activeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = stringResource(R.string.stats_total_created),
            value = totalCreated.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.stats_completed),
            value = totalCompleted.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.stats_completion_rate),
            value = "${(completionRate * 100).toInt()}%",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.stats_active_tasks),
            value = activeCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                Color.Transparent
            )
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(brush = gradientBrush) }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Column {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayRangeToggle(
    selectedDays: Int,
    onSelect: (Int) -> Unit
) {
    val options = listOf(
        7 to stringResource(R.string.stats_7d),
        14 to stringResource(R.string.stats_14d),
        30 to stringResource(R.string.stats_30d)
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (days, label) ->
            SegmentedButton(
                selected = selectedDays == days,
                onClick = { onSelect(days) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DailyChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.second } ?: 0
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (maxValue == 0) return@Canvas

            val leftPadding = 32.dp.toPx()
            val bottomPadding = 24.dp.toPx()
            val topPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - bottomPadding - topPadding
            val barCount = data.size
            val barSpacing = chartWidth / barCount
            val barWidth = (barSpacing * 0.6f).coerceAtMost(24.dp.toPx())

            for (i in 0 until 4) {
                val y = topPadding + chartHeight * (1f - i / 3f)
                drawLine(
                    color = surfaceColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1.dp.toPx()
                )
                if (i > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${(maxValue * i / 3)}",
                        leftPadding - 6.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            color = onSurface.copy(alpha = 0.5f).hashCode()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }

            for ((index, pair) in data.withIndex()) {
                val value = pair.second
                val barHeight = if (maxValue > 0) (value.toFloat() / maxValue) * chartHeight else 0f
                val x = leftPadding + index * barSpacing + (barSpacing - barWidth) / 2
                val y = topPadding + chartHeight - barHeight

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.2f)
                        ),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                if (index % 2 == 0 || index == data.lastIndex) {
                    val label = pair.first.substringAfterLast("-", pair.first)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x + barWidth / 2,
                        size.height - 2.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 9.sp.toPx()
                            color = onSurface.copy(alpha = 0.6f).hashCode()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeatmap(data: List<Pair<String, Int>>) {
    val maxCount = data.maxOfOrNull { it.second } ?: 1
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEachIndexed { index, (name, count) ->
                val intensity = if (maxCount > 0) count.toFloat() / maxCount else 0f
                val isWeekend = index >= 5
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (count > 0) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f + 0.6f * intensity)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWeekend) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (count > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSection(
    data: List<Pair<String, Int>>,
    tagColorMap: Map<String, Int>
) {
    val total = data.sumOf { it.second }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(140.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (total == 0) return@Canvas
                    val strokeWidth = 28.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    var startAngle = -90f
                    for ((index, pair) in data.withIndex()) {
                        val sweep = (pair.second.toFloat() / total) * 360f
                        val colorIndex = tagColorMap[pair.first] ?: (index % tagPalette.size)
                        drawArc(
                            color = tagPalette[colorIndex % tagPalette.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweep
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "$total",
                        size.width / 2f,
                        size.height / 2f + 6.dp.toPx(),
                        android.graphics.Paint().apply {
                            textSize = 20.sp.toPx()
                            color = onSurfaceColor.hashCode()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.forEachIndexed { index, pair ->
                    val pct = (pair.second.toFloat() / total * 100).toInt()
                    val colorIndex = tagColorMap[pair.first] ?: (index % tagPalette.size)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .padding(end = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(tagPalette[colorIndex % tagPalette.size])
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${pair.first} $pct%",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrioritySection(data: List<Pair<Int, Int>>) {
    val maxCount = data.maxOfOrNull { it.second } ?: 1
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.reversed().forEach { (level, count) ->
                val fraction = count.toFloat() / maxCount
                val barColor = when (level) {
                    0 -> MaterialTheme.colorScheme.surfaceVariant
                    1 -> Color(0xFFA5D6A7)
                    2 -> Color(0xFFFFF176)
                    3 -> Color(0xFFFFAB91)
                    4 -> Color(0xFFEF9A9A)
                    5 -> Color(0xFFE53935)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val stars = when (level) {
                    0 -> "None"
                    1 -> "\u2605"
                    2 -> "\u2605\u2605"
                    3 -> "\u2605\u2605\u2605"
                    4 -> "\u2605\u2605\u2605\u2605"
                    5 -> "\u2605\u2605\u2605\u2605\u2605"
                    else -> ""
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stars, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$count",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = surfaceVariantColor,
                                size = size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width * fraction, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakSection(
    streak: Int,
    gridData: List<Pair<String, Int>>
) {
    val weeks = gridData.chunked(7)
    if (weeks.isEmpty()) return

    val cellSize = 10.dp
    val cellGap = 3.dp
    val rowLabelWidth = 26.dp
    val maxCount = gridData.maxOfOrNull { it.second } ?: 0

    val monthLabels = remember(gridData) {
        val wk = gridData.chunked(7)
        val labels = mutableListOf<Pair<Int, String>>()
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        wk.forEachIndexed { col, week ->
            if (week.isNotEmpty()) {
                val cal = DateHelper.parseDate(week[0].first)
                val m = cal.get(Calendar.MONTH)
                if (labels.isEmpty() || labels.last().second != months[m]) {
                    labels.add(col to months[m])
                }
            }
        }
        labels.toList()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\uD83D\uDD25 $streak ${stringResource(R.string.stats_days)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(Modifier.width(rowLabelWidth))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    weeks.indices.forEach { col ->
                        val label = monthLabels.find { it.first == col }
                        Box(
                            modifier = Modifier
                                .width(cellSize)
                                .height(12.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            if (label != null) {
                                Text(
                                    text = label.second,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val rowLabelEntries = listOf(0 to "Mon", 2 to "Wed", 4 to "Fri")

            for (row in 0..6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val entry = rowLabelEntries.find { it.first == row }
                    Text(
                        text = entry?.second ?: "",
                        modifier = Modifier.width(rowLabelWidth),
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        weeks.forEach { week ->
                            if (row < week.size) {
                                val count = week[row].second
                                val color = when {
                                    count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                    maxCount <= 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    count.toFloat() / maxCount < 0.33f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    count.toFloat() / maxCount < 0.66f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .background(color, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueSection(
    overdueCount: Int,
    overdueByPriority: Map<Int, Int>
) {
    val high = overdueByPriority.filterKeys { it >= 4 }.values.sum()
    val medium = overdueByPriority.filterKeys { it in 2..3 }.values.sum()
    val low = overdueByPriority.filterKeys { it <= 1 }.values.sum()

    val breakdown = buildList {
        if (high > 0) add("$high ${stringResource(R.string.stats_high)}")
        if (medium > 0) add("$medium ${stringResource(R.string.stats_medium)}")
        if (low > 0) add("$low ${stringResource(R.string.stats_low)}")
    }.joinToString(" \u00B7 ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (overdueCount > 0) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = overdueCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (overdueCount > 0) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "${stringResource(R.string.stats_overdue)} ${stringResource(R.string.stats_tasks)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (breakdown.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = breakdown,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
