package com.organizo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizo.app.data.Note
import com.organizo.app.viewmodel.NoteEditorViewModel
import com.organizo.app.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(onOpen: (Long) -> Unit, vm: NotesViewModel = viewModel()) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = { OrgTopBar("Notes") },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpen(0L) }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, "New note")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search notes") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Close, "Clear search") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            if (notes.isEmpty()) {
                EmptyState(
                    Icons.Filled.StickyNote2,
                    if (query.isBlank()) "No notes yet" else "No matching notes",
                    if (query.isBlank()) "Tap + to write your first note." else "Try a different search."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { n ->
                        NoteCard(n, onClick = { onOpen(n.id) }, onPin = { vm.togglePin(n) }, onDelete = { toDelete = n })
                    }
                }
            }
        }
    }

    toDelete?.let { n ->
        ConfirmDialog(
            title = "Delete note?",
            message = "\"${n.title.ifBlank { "Untitled" }}\" will be permanently deleted.",
            onConfirm = { vm.delete(n.id); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun NoteCard(n: Note, onClick: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = orgCardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(
                    n.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (n.content.isNotBlank()) {
                    Text(
                        n.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(n.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column {
                IconButton(onClick = onPin) {
                    if (n.pinned) Icon(Icons.Filled.PushPin, "Unpin", tint = MaterialTheme.colorScheme.primary)
                    else Icon(Icons.Outlined.PushPin, "Pin")
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete note") }
            }
        }
    }
}

@Composable
fun NoteEditorScreen(id: Long, onBack: () -> Unit, vm: NoteEditorViewModel = viewModel()) {
    LaunchedEffect(id) { vm.load(id) }
    DisposableEffect(Unit) { onDispose { vm.flush() } }
    var confirmDelete by remember { mutableStateOf(false) }
    val transparent = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    Scaffold(
        topBar = {
            OrgTopBar(
                title = if (id == 0L) "New note" else "Edit note",
                onBack = onBack,
                actions = {
                    if (vm.status.isNotEmpty()) {
                        Text(vm.status, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = { vm.togglePin() }) {
                        if (vm.pinned) Icon(Icons.Filled.PushPin, "Unpin note")
                        else Icon(Icons.Outlined.PushPin, "Pin note")
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete note") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding()) {
            TextField(
                value = vm.title,
                onValueChange = vm::onTitle,
                placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                colors = transparent,
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = vm.content,
                onValueChange = vm::onContent,
                placeholder = { Text("Start typing…") },
                colors = transparent,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete note?",
            message = "This note will be permanently deleted.",
            onConfirm = { confirmDelete = false; vm.delete(); onBack() },
            onDismiss = { confirmDelete = false }
        )
    }
}
