package com.organizo.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.organizo.app.data.Note
import com.organizo.app.data.OrganizoDb
import com.organizo.app.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = OrganizoDb.get(app)
    val noteCount: StateFlow<Int> =
        db.noteDao().count().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val pendingTasks: StateFlow<Int> =
        db.taskDao().pendingCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = OrganizoDb.get(app).noteDao()
    val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = query
        .flatMapLatest { dao.search(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        query.value = q
    }

    fun togglePin(n: Note) {
        viewModelScope.launch { dao.setPinned(n.id, !n.pinned) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { withContext(NonCancellable) { dao.deleteById(id) } }
    }
}

/** Editor with debounced auto-save: every edit is written to Room 0.6s after you stop typing. */
class NoteEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = OrganizoDb.get(app).noteDao()
    private var noteId = 0L
    private var started = false
    private var dirty = false
    private var discarded = false
    private var saveJob: Job? = null

    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set
    var pinned by mutableStateOf(false)
        private set
    var status by mutableStateOf("")
        private set

    fun load(id: Long) {
        if (started) return
        started = true
        noteId = id
        if (id > 0) {
            viewModelScope.launch {
                dao.get(id)?.let {
                    title = it.title
                    content = it.content
                    pinned = it.pinned
                }
            }
        }
    }

    fun onTitle(v: String) {
        title = v
        scheduleSave()
    }

    fun onContent(v: String) {
        content = v
        scheduleSave()
    }

    fun togglePin() {
        pinned = !pinned
        scheduleSave()
    }

    private fun scheduleSave() {
        dirty = true
        status = "Saving…"
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            withContext(NonCancellable) { persist() }
        }
    }

    private suspend fun persist() {
        if (!dirty || discarded) return
        dirty = false
        if (title.isBlank() && content.isBlank() && noteId == 0L) {
            status = ""
            return
        }
        val n = Note(id = noteId, title = title, content = content, pinned = pinned, updatedAt = System.currentTimeMillis())
        if (noteId == 0L) noteId = dao.insert(n) else dao.update(n)
        status = "Saved"
    }

    fun flush() {
        saveJob?.cancel()
        viewModelScope.launch { withContext(NonCancellable) { persist() } }
    }

    fun delete() {
        discarded = true
        saveJob?.cancel()
        val id = noteId
        if (id > 0) viewModelScope.launch { withContext(NonCancellable) { dao.deleteById(id) } }
    }
}

class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = OrganizoDb.get(app).taskDao()
    val tasks: StateFlow<List<Task>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(t: Task) {
        viewModelScope.launch { if (t.id == 0L) dao.insert(t) else dao.update(t) }
    }

    fun toggle(t: Task) = save(t.copy(completed = !t.completed))

    fun delete(t: Task) {
        viewModelScope.launch { dao.deleteById(t.id) }
    }
}

class PdfViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app
    private val mutex = Mutex()
    private var renderer: PdfRenderer? = null

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var fileName by mutableStateOf("")
        private set
    var pageCount by mutableIntStateOf(0)
        private set
    var docId by mutableIntStateOf(0)
        private set

    fun open(uri: Uri) {
        viewModelScope.launch {
            loading = true
            error = null
            pageCount = 0
            try {
                val name = withContext(Dispatchers.IO) { displayName(ctx, uri) }
                var pages = 0
                mutex.withLock {
                    withContext(Dispatchers.IO) {
                        renderer?.close()
                        renderer = null
                        val f = File(ctx.cacheDir, "current.pdf")
                        val input = ctx.contentResolver.openInputStream(uri)
                            ?: throw IOException("Unable to open the selected file.")
                        input.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                        val r = PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY))
                        renderer = r
                        pages = r.pageCount
                    }
                }
                fileName = name
                pageCount = pages
                docId++
            } catch (e: SecurityException) {
                error = "Password-protected PDFs are not supported."
            } catch (e: Exception) {
                error = "Could not open this PDF. The file may be damaged or is not a valid PDF."
            }
            loading = false
        }
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val r = renderer ?: return@withContext null
            if (index !in 0 until r.pageCount) return@withContext null
            try {
                r.openPage(index).use { page ->
                    val h = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCleared() {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        renderer = null
    }
}

sealed interface DocContent {
    data class Table(val rows: List<List<String>>) : DocContent
    data class TextDoc(val paragraphs: List<String>) : DocContent
}

sealed interface DocState {
    object Idle : DocState
    object Loading : DocState
    data class Error(val message: String) : DocState
    data class Loaded(val name: String, val content: DocContent, val note: String?) : DocState
}

class DocsViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app

    var state by mutableStateOf<DocState>(DocState.Idle)
        private set

    fun open(uri: Uri) {
        state = DocState.Loading
        viewModelScope.launch {
            state = try {
                withContext(Dispatchers.IO) { load(uri) }
            } catch (e: DocException) {
                DocState.Error(e.message ?: "Could not open this file.")
            } catch (e: Exception) {
                DocState.Error("Could not read this file. It may be damaged, password-protected or in an unsupported format.")
            }
        }
    }

    private fun load(uri: Uri): DocState {
        val name = displayName(ctx, uri)
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "csv", "tsv" -> {
                val rows = parseCsv(readText(ctx, uri), MAX_ROWS + 1)
                tableState(name, rows)
            }
            "xlsx" -> tableState(name, parseXlsx(ctx, uri, MAX_ROWS + 1))
            "docx" -> {
                val paras = parseDocx(ctx, uri, MAX_ROWS + 1)
                if (paras.isEmpty()) throw DocException("This document appears to be empty.")
                val cut = paras.size > MAX_ROWS
                DocState.Loaded(name, DocContent.TextDoc(paras.take(MAX_ROWS)), if (cut) "Showing the first $MAX_ROWS paragraphs." else null)
            }
            "txt" -> {
                val lines = readText(ctx, uri).lines()
                DocState.Loaded(name, DocContent.TextDoc(lines.take(MAX_ROWS)), if (lines.size > MAX_ROWS) "Showing the first $MAX_ROWS lines." else null)
            }
            "xls", "doc" -> throw DocException(
                "Old .$ext files are not supported. Open it in Excel/Word, save it as .xlsx/.docx, then try again."
            )
            else -> throw DocException("Unsupported file type. Please choose a .csv, .xlsx or .docx file.")
        }
    }

    private fun tableState(name: String, rows: List<List<String>>): DocState {
        if (rows.isEmpty()) throw DocException("This file appears to be empty.")
        val cut = rows.size > MAX_ROWS
        return DocState.Loaded(name, DocContent.Table(rows.take(MAX_ROWS)), if (cut) "Showing the first $MAX_ROWS rows." else null)
    }
}
