package com.organizo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.organizo.app.data.SettingsStore

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val store = remember { SettingsStore.get(ctx) }
    val mode by store.mode.collectAsState()
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    Scaffold(topBar = { OrgTopBar("Settings") }) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Card(shape = RoundedCornerShape(16.dp), colors = orgCardColors(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    listOf("System default", "Light mode", "Dark mode").forEachIndexed { i, label ->
                        Row(
                            Modifier.fillMaxWidth().clickable { store.setMode(i) }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = mode == i, onClick = { store.setMode(i) })
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Text("About", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Card(shape = RoundedCornerShape(16.dp), colors = orgCardColors(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow("App", "Organizo")
                    InfoRow("Version", version)
                    InfoRow("Package", ctx.packageName)
                    InfoRow("Storage", "Notes and tasks are stored only on this device. Organizo works fully offline.")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
