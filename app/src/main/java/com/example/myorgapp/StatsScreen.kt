package com.example.myorgapp

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: SharedCardViewModel,
    onBack: () -> Unit
) {
    val streak by viewModel.streak.collectAsState()
    viewModel.cards.collectAsState()
    viewModel.completedCards.collectAsState()

    val totalCreated = viewModel.getTotalCreated()
    val totalCompleted = viewModel.getTotalCompleted()
    val completionRate = viewModel.getCompletionRate()
    val activeCount = viewModel.getActiveCount()
    val dailyData = viewModel.getDailyCompletions(14)
    val tagData = viewModel.getTagDistribution()
    val priorityData = viewModel.getPriorityDistribution()
    val overdueCount = viewModel.getOverdueCount()

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryRow(
                    totalCreated = totalCreated,
                    totalCompleted = totalCompleted,
                    completionRate = completionRate,
                    activeCount = activeCount
                )

                SectionTitle(stringResource(R.string.stats_daily))
                DailyChart(
                    data = dailyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                if (tagData.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.stats_by_tag))
                    TagSection(tagData)
                }

                if (priorityData.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.stats_by_priority))
                    PrioritySection(priorityData)
                }

                BottomRow(
                    overdueCount = overdueCount,
                    streak = streak
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    color = primaryColor.copy(alpha = 0.7f + 0.3f * (value.toFloat() / maxValue.coerceAtLeast(1))),
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
private fun TagSection(data: List<Pair<String, Int>>) {
    val total = data.sumOf { it.second }
    val chartColors = listOf(
        Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
        Color(0xFFD81B60), Color(0xFF00ACC1)
    )

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        drawArc(
                            color = chartColors[index % chartColors.size],
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
                    data.take(6).forEachIndexed { index, pair ->
                        val pct = (pair.second.toFloat() / total * 100).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .padding(end = 4.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(chartColors[index % chartColors.size])
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
                if (data.size > 6) {
                    Text(
                        "+${data.size - 6} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
private fun BottomRow(
    overdueCount: Int,
    streak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
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
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$streak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${stringResource(R.string.stats_streak)} ${stringResource(R.string.stats_days)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
