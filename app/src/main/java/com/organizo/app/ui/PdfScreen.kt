package com.organizo.app.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizo.app.viewmodel.PdfViewModel

@Composable
fun PdfScreen(onBack: () -> Unit, vm: PdfViewModel = viewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri)
    }
    val openPdf = { picker.launch(arrayOf("application/pdf")) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val err = vm.error

    Scaffold(
        topBar = {
            OrgTopBar(
                title = vm.fileName.ifBlank { "PDF Manager" },
                onBack = onBack,
                actions = {
                    if (vm.pageCount > 0) {
                        IconButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f) }, enabled = zoom > 1f) {
                            Icon(Icons.Filled.ZoomOut, "Zoom out")
                        }
                        Text("${(zoom * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(3f) }, enabled = zoom < 3f) {
                            Icon(Icons.Filled.ZoomIn, "Zoom in")
                        }
                    }
                    IconButton(onClick = { openPdf() }) { Icon(Icons.Filled.FolderOpen, "Open PDF") }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                vm.loading -> CircularProgressIndicator()
                err != null -> EmptyState(Icons.Filled.Error, "Can't open PDF", err, "Choose another file") { openPdf() }
                vm.pageCount == 0 -> EmptyState(
                    Icons.Filled.PictureAsPdf,
                    "No PDF open",
                    "Pick a PDF from your phone storage to read it. Use the + / − buttons to zoom.",
                    "Open PDF"
                ) { openPdf() }
                else -> PdfViewer(vm, zoom)
            }
        }
    }
}

@Composable
private fun PdfViewer(vm: PdfViewModel, zoom: Float) {
    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        val widthDp = maxWidth * zoom
        val widthPx = with(LocalDensity.current) { widthDp.roundToPx() }
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(widthDp).fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.pageCount) { index -> PdfPage(vm, vm.docId, index, widthPx) }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            shape = RoundedCornerShape(50),
            color = Color(0xCC0A2342)
        ) {
            Text(
                "$currentPage / ${vm.pageCount}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PdfPage(vm: PdfViewModel, docId: Int, index: Int, widthPx: Int) {
    var bitmap by remember(docId, index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(docId, index, widthPx) {
        vm.render(index, widthPx)?.let { bitmap = it }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
