package com.organizo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizo.app.data.Task
import com.organizo.app.viewmodel.TasksViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(vm: TasksViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val byDay = remember(tasks) { tasks.filter { it.dueDate != null }.groupBy { it.dueDate!! } }
    val today = LocalDate.now()

    Scaffold(topBar = { OrgTopBar("Calendar") }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = orgCardColors(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, "Next month") }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                            Text(
                                d,
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val offset = month.atDay(1).dayOfWeek.value % 7
                    val days = month.lengthOfMonth()
                    val rows = (offset + days + 6) / 7
                    for (r in 0 until rows) {
                        Row(Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val dayNum = r * 7 + c - offset + 1
                                if (dayNum in 1..days) {
                                    val date = month.atDay(dayNum)
                                    DayCell(
                                        date = date,
                                        isSelected = date == selected,
                                        isToday = date == today,
                                        dayTasks = byDay[date.toEpochDay()],
                                        modifier = Modifier.weight(1f),
                                        onClick = { selected = date }
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                    TextButton(onClick = { month = YearMonth.now(); selected = today }) { Text("Today") }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Deadlines · " + selected.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            val dayTasks = byDay[selected.toEpochDay()].orEmpty()
            if (dayTasks.isEmpty()) {
                Text("No task deadlines on this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dayTasks.forEach { t -> DayTaskRow(t) { vm.toggle(t) } }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    dayTasks: List<Task>?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val dot = when {
        dayTasks == null -> null
        dayTasks.any { !it.completed } ->
            if (date.isBefore(LocalDate.now())) Color(0xFFE53935) else Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }
    Box(
        modifier.aspectRatio(1f).padding(2.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                color = fg,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium
            )
            if (dot != null) Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        }
    }
}

@Composable
private fun DayTaskRow(t: Task, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = orgCardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = t.completed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (t.completed) TextDecoration.LineThrough else null
                )
            }
            Box(Modifier.padding(end = 12.dp)) { PriorityBadge(t.priority) }
        }
    }
}
