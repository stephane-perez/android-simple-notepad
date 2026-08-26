package com.stephaneperez.notepad.data

import android.net.Uri

/** Pending action gated behind the unsaved-changes dialog. */
enum class PendingAction { NEW, OPEN }

data class NotepadUiState(
    val filename: String = "untitled.txt",
    val uri: Uri? = null,
    val text: String = "",
    val dirty: Boolean = false,
    val wrap: Boolean = false,
    val lineNumbers: Boolean = true,
    val finding: Boolean = false,
    val query: String = "",
    val replacement: String = "",
    val matchIndex: Int = 0,
    val menuOpen: Boolean = false,
    val browsing: Boolean = false,
    val confirm: PendingAction? = null,
    val toast: String = "",
    val filePath: String = "/Documents/notes/untitled.txt",
) {
    /** Derived, not stored: line count. */
    val lineCount: Int get() = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1

    /** Derived: char count. */
    val charCount: Int get() = text.length

    /** Derived: gutter visibility — hidden whenever word wrap is on. */
    val gutterVisible: Boolean get() = lineNumbers && !wrap

    /** Derived: total match count for the current literal, case-sensitive query. */
    val totalMatches: Int
        get() {
            if (query.isEmpty()) return 0
            var count = 0
            var index = text.indexOf(query)
            while (index >= 0) {
                count++
                index = text.indexOf(query, index + query.length)
            }
            return count
        }

    /** Derived: `3 / 7`, `none`, or empty when the query is empty. */
    val matchLabel: String
        get() {
            if (query.isEmpty()) return ""
            val total = totalMatches
            return if (total == 0) "none" else "${matchIndex + 1} / $total"
        }

    val counts: String
        get() {
            val lines = lineCount
            val lineWord = if (lines == 1) "line" else "lines"
            return "$lines $lineWord · $charCount chars"
        }
}
