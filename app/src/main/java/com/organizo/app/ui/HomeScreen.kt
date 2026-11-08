package com.organizo.app.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizo.app.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class QuickItem(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

@Composable
fun HomeScreen(onOpen: (String) -> Unit, vm: HomeViewModel = viewModel()) {
    val notes by vm.noteCount.collectAsStateWithLifecycle()
    val pending by vm.pendingTasks.collectAsStateWithLifecycle()
    val items = listOf(
        QuickItem("notes", "Notes", "$notes saved", Icons.Filled.StickyNote2),
        QuickItem("tasks", "Tasks", "$pending pending", Icons.Filled.TaskAlt),
        QuickItem("calendar", "Calendar", "Deadlines", Icons.Filled.CalendarMonth),
        QuickItem("pdf", "PDF", "Read & zoom", Icons.Filled.PictureAsPdf),
        QuickItem("docs", "Documents", "Excel, CSV, Word", Icons.Filled.Description)
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(barColor(), Color(0xFF1B3A6B))))
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Text("Organizo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("Welcome back! Let's get organized today.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(12.dp))
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Quick access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        items.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { item -> QuickCard(item, Modifier.weight(1f)) { onOpen(item.route) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickCard(item: QuickItem, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = orgCardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
