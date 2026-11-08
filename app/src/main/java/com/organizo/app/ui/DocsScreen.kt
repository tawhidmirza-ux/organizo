package com.organizo.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizo.app.viewmodel.DocContent
import com.organizo.app.viewmodel.DocState
import com.organizo.app.viewmodel.DocsViewModel

@Composable
fun DocsScreen(onBack: () -> Unit, vm: DocsViewModel = viewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri)
    }
    val openDoc = { picker.launch(arrayOf("*/*")) }
    val st = vm.state

    Scaffold(
        topBar = {
            OrgTopBar(
                title = if (st is DocState.Loaded) st.name else "Documents",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { openDoc() }) { Icon(Icons.Filled.FolderOpen, "Open document") }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            when (st) {
                DocState.Idle -> EmptyState(
                    Icons.Filled.Description,
                    "No document open",
                    "Open an Excel (.xlsx), CSV or Word (.docx) file from your phone to view its content.",
                    "Open document"
                ) { openDoc() }
                DocState.Loading -> CircularProgressIndicator()
                is DocState.Error -> EmptyState(Icons.Filled.Error, "Can't open file", st.message, "Choose another file") { openDoc() }
                is DocState.Loaded -> LoadedDoc(st)
            }
        }
    }
}

@Composable
private fun LoadedDoc(st: DocState.Loaded) {
    Column(Modifier.fillMaxSize()) {
        if (st.note != null) {
            Text(
                st.note,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(10.dp)
            )
        }
        when (val c = st.content) {
            is DocContent.Table -> TableView(c.rows)
            is DocContent.TextDoc -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(c.paragraphs) { _, p ->
                    Text(
                        p.ifBlank { " " },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableView(rows: List<List<String>>) {
    val cols = rows.maxOfOrNull { it.size } ?: 0
    val colWidth = 140
    Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        LazyColumn(Modifier.width((colWidth * cols).dp)) {
            itemsIndexed(rows) { i, row ->
                val bg = when {
                    i == 0 -> MaterialTheme.colorScheme.primaryContainer
                    i % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
                Row(Modifier.background(bg)) {
                    for (c in 0 until cols) {
                        Text(
                            row.getOrElse(c) { "" },
                            modifier = Modifier.width(colWidth.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
