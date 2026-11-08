package com.organizo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizo.app.data.Task
import com.organizo.app.viewmodel.TasksViewModel
import java.time.LocalDate

@Composable
fun TasksScreen(vm: TasksViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var toDelete by remember { mutableStateOf<Task?>(null) }

    val shown = when (filter) {
        1 -> tasks.filter { !it.completed }
        2 -> tasks.filter { it.completed }
        else -> tasks
    }

    Scaffold(
        topBar = { OrgTopBar("Tasks") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; dialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Filled.Add, "Add task") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Pending", "Completed").forEachIndexed { i, label ->
                    FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) })
                }
            }
            if (shown.isEmpty()) {
                EmptyState(
                    Icons.Filled.TaskAlt,
                    if (tasks.isEmpty()) "No tasks yet" else "Nothing here",
                    if (tasks.isEmpty()) "Tap + to add your first task." else "No tasks match this filter."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { t ->
                        TaskCard(
                            t,
                            onToggle = { vm.toggle(t) },
                            onEdit = { editing = t; dialogOpen = true },
                            onDelete = { toDelete = t }
                        )
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        TaskDialog(
            initial = editing,
            onSave = { vm.save(it); dialogOpen = false },
            onDismiss = { dialogOpen = false }
        )
    }
    toDelete?.let { t ->
        ConfirmDialog(
            title = "Delete task?",
            message = "\"${t.title}\" will be permanently deleted.",
            onConfirm = { vm.delete(t); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun TaskCard(t: Task, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val overdue = !t.completed && t.dueDate != null && t.dueDate < LocalDate.now().toEpochDay()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = orgCardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = t.completed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (t.completed) TextDecoration.LineThrough else null,
                    color = if (t.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (t.description.isNotBlank()) {
                    Text(
                        t.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityBadge(t.priority)
                    if (t.dueDate != null) {
                        Text(
                            (if (overdue) "Overdue · " else "Due ") + fmtDay(t.dueDate),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit task") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete task") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDialog(initial: Task?, onSave: (Task) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var priority by remember { mutableIntStateOf(initial?.priority ?: 1) }
    var due by remember { mutableStateOf(initial?.dueDate) }
    var picker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Title") },
                    isError = titleError,
                    supportingText = { if (titleError) Text("Please enter a title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityLabels.forEachIndexed { i, label ->
                        FilterChip(selected = priority == i, onClick = { priority = i }, label = { Text(label) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { picker = true }) {
                        Icon(Icons.Filled.CalendarMonth, null)
                        Spacer(Modifier.width(8.dp))
                        Text(due?.let { fmtDay(it) } ?: "Set due date")
                    }
                    if (due != null) {
                        IconButton(onClick = { due = null }) { Icon(Icons.Filled.Close, "Clear due date") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    titleError = true
                } else {
                    onSave(
                        (initial ?: Task(title = "")).copy(
                            title = title.trim(),
                            description = desc.trim(),
                            priority = priority,
                            dueDate = due
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (picker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = due?.let { it * 86_400_000L })
        DatePickerDialog(
            onDismissRequest = { picker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { due = Math.floorDiv(it, 86_400_000L) }
                    picker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
