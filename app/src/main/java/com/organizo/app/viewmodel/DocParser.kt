package com.organizo.app.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class DocException(message: String) : Exception(message)

const val MAX_ROWS = 2000

fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && c.moveToFirst()) return c.getString(i) ?: "document"
    }
    return uri.lastPathSegment ?: "document"
}

fun readText(context: Context, uri: Uri): String {
    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: throw DocException("Unable to open the selected file.")
    return text.removePrefix("\uFEFF")
}

private fun readEntries(context: Context, uri: Uri, wanted: (String) -> Boolean): Map<String, ByteArray> {
    val out = mutableMapOf<String, ByteArray>()
    val stream = context.contentResolver.openInputStream(uri)
        ?: throw DocException("Unable to open the selected file.")
    stream.use { raw ->
        ZipInputStream(raw.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (wanted(entry.name)) out[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
    }
    return out
}

private fun XmlPullParser.local(): String = (name ?: "").substringAfter(':')

fun parseCsv(text: String, maxRows: Int): List<List<String>> {
    val first = text.lineSequence().firstOrNull().orEmpty()
    val delimiter = listOf(',', ';', '\t').maxByOrNull { d -> first.count { it == d } } ?: ','
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length && rows.size < maxRows) {
        val c = text[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < text.length && text[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                sb.append(c)
            }
        } else if (c == '"') {
            inQuotes = true
        } else if (c == delimiter) {
            row.add(sb.toString())
            sb.setLength(0)
        } else if (c == '\n') {
            row.add(sb.toString())
            sb.setLength(0)
            rows.add(row)
            row = mutableListOf()
        } else if (c != '\r') {
            sb.append(c)
        }
        i++
    }
    if ((sb.isNotEmpty() || row.isNotEmpty()) && rows.size < maxRows) {
        row.add(sb.toString())
        rows.add(row)
    }
    return rows
}

private fun colIndex(ref: String): Int {
    var n = 0
    for (ch in ref) {
        if (ch in 'A'..'Z') n = n * 26 + (ch - 'A' + 1) else break
    }
    return (n - 1).coerceAtLeast(0)
}

private fun parseShared(bytes: ByteArray): List<String> {
    val p = Xml.newPullParser()
    p.setInput(ByteArrayInputStream(bytes), null)
    val out = mutableListOf<String>()
    var sb = StringBuilder()
    var inT = false
    var ev = p.eventType
    while (ev != XmlPullParser.END_DOCUMENT) {
        if (ev == XmlPullParser.START_TAG) {
            val n = p.local()
            if (n == "si") sb = StringBuilder()
            if (n == "t") inT = true
        } else if (ev == XmlPullParser.TEXT) {
            if (inT) sb.append(p.text)
        } else if (ev == XmlPullParser.END_TAG) {
            val n = p.local()
            if (n == "t") inT = false
            if (n == "si") out.add(sb.toString())
        }
        ev = p.next()
    }
    return out
}

private fun parseSheet(bytes: ByteArray, shared: List<String>, maxRows: Int): List<List<String>> {
    val p = Xml.newPullParser()
    p.setInput(ByteArrayInputStream(bytes), null)
    val rows = mutableListOf<List<String>>()
    var row: MutableList<String>? = null
    var type = ""
    var col = 0
    var value = StringBuilder()
    var capture = false
    var ev = p.eventType
    while (ev != XmlPullParser.END_DOCUMENT && rows.size < maxRows) {
        if (ev == XmlPullParser.START_TAG) {
            when (p.local()) {
                "row" -> row = mutableListOf()
                "c" -> {
                    type = p.getAttributeValue(null, "t") ?: ""
                    val ref = p.getAttributeValue(null, "r")
                    col = if (ref != null) colIndex(ref) else (row?.size ?: 0)
                    value = StringBuilder()
                }
                "v", "t" -> capture = true
            }
        } else if (ev == XmlPullParser.TEXT) {
            if (capture) value.append(p.text)
        } else if (ev == XmlPullParser.END_TAG) {
            when (p.local()) {
                "v", "t" -> capture = false
                "c" -> {
                    val txt = when (type) {
                        "s" -> shared.getOrNull(value.toString().trim().toIntOrNull() ?: -1) ?: ""
                        "b" -> if (value.toString().trim() == "1") "TRUE" else "FALSE"
                        else -> value.toString()
                    }
                    val r = row
                    if (r != null) {
                        while (r.size < col) r.add("")
                        if (r.size == col) r.add(txt) else r[col] = txt
                    }
                }
                "row" -> {
                    val r = row
                    if (r != null) rows.add(r)
                    row = null
                }
            }
        }
        ev = p.next()
    }
    return rows
}

fun parseXlsx(context: Context, uri: Uri, maxRows: Int): List<List<String>> {
    val files = readEntries(context, uri) {
        it == "xl/sharedStrings.xml" || (it.startsWith("xl/worksheets/") && it.endsWith(".xml"))
    }
    val sheetName = if ("xl/worksheets/sheet1.xml" in files) {
        "xl/worksheets/sheet1.xml"
    } else {
        files.keys.filter { it.startsWith("xl/worksheets/") }.sorted().firstOrNull()
    } ?: throw DocException("No worksheet found. This doesn't look like a valid .xlsx file.")
    val shared = files["xl/sharedStrings.xml"]?.let { parseShared(it) } ?: emptyList()
    val sheetBytes = files[sheetName] ?: throw DocException("Worksheet could not be read.")
    return parseSheet(sheetBytes, shared, maxRows)
}

fun parseDocx(context: Context, uri: Uri, maxParagraphs: Int): List<String> {
    val files = readEntries(context, uri) { it == "word/document.xml" }
    val bytes = files["word/document.xml"]
        ?: throw DocException("This doesn't look like a valid .docx file.")
    val p = Xml.newPullParser()
    p.setInput(ByteArrayInputStream(bytes), null)
    val paragraphs = mutableListOf<String>()
    var sb = StringBuilder()
    var inT = false
    var ev = p.eventType
    while (ev != XmlPullParser.END_DOCUMENT && paragraphs.size < maxParagraphs) {
        if (ev == XmlPullParser.START_TAG) {
            when (p.local()) {
                "p" -> sb = StringBuilder()
                "t" -> inT = true
                "tab" -> sb.append('\t')
                "br" -> sb.append('\n')
            }
        } else if (ev == XmlPullParser.TEXT) {
            if (inT) sb.append(p.text)
        } else if (ev == XmlPullParser.END_TAG) {
            when (p.local()) {
                "t" -> inT = false
                "p" -> paragraphs.add(sb.toString())
            }
        }
        ev = p.next()
    }
    return paragraphs
}

