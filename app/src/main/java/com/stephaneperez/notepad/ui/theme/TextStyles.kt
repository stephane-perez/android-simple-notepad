package com.stephaneperez.notepad.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Tabular figures wherever numbers stand as numbers (counts, match counter, gutter, meta),
 *  applied everywhere via the "tnum" OpenType feature below. */
object NotepadType {
    // Chrome headings — Cormorant Garamond 400
    val dialogTitle = TextStyle(
        fontFamily = CormorantGaramond,
        fontSize = 22.sp,
        color = NotepadColors.ink
    )
    val appBarTitle = TextStyle(
        fontFamily = CormorantGaramond,
        fontSize = 19.sp,
        color = NotepadColors.ink
    )
    val overlayTitle = appBarTitle

    // Body & controls — Lora 400
    val body = TextStyle(
        fontFamily = Lora,
        fontSize = 14.sp,
        lineHeight = 22.4.sp, // 14sp * 1.6
        color = NotepadColors.ink
    )
    val button = TextStyle(
        fontFamily = Lora,
        fontSize = 13.sp,
        color = NotepadColors.ink
    )
    val counter = TextStyle(
        fontFamily = Lora,
        fontSize = 12.sp,
        fontFeatureSettings = "tnum",
        color = NotepadColors.inkMeta
    )
    val metaAndStatus = TextStyle(
        fontFamily = Lora,
        fontSize = 11.sp,
        fontFeatureSettings = "tnum",
        color = NotepadColors.inkStatusBar
    )
    val menuLabel = TextStyle(
        fontFamily = Lora,
        fontSize = 14.sp,
        color = NotepadColors.ink
    )

    // Uppercase micro-labels
    val microLabel = TextStyle(
        fontFamily = Lora,
        fontSize = 11.sp,
        letterSpacing = 0.06.em,
        color = NotepadColors.accent700
    )

    // Editor, filenames, paths, toast — monospace
    val editorText = TextStyle(
        fontFamily = NotepadMono,
        fontSize = 15.sp,
        lineHeight = 23.25.sp, // 15sp * 1.55
        fontFeatureSettings = "tnum",
        color = NotepadColors.ink
    )
    val gutter = TextStyle(
        fontFamily = NotepadMono,
        fontSize = 15.sp,
        lineHeight = 23.25.sp,
        fontFeatureSettings = "tnum",
        color = NotepadColors.inkGutter
    )
    val fieldOrFilename = TextStyle(
        fontFamily = NotepadMono,
        fontSize = 14.sp,
        color = NotepadColors.ink
    )
    val path = TextStyle(
        fontFamily = NotepadMono,
        fontSize = 11.sp,
        color = NotepadColors.inkStatusBar
    )
    val toast = TextStyle(
        fontFamily = NotepadMono,
        fontSize = 13.sp,
        color = NotepadColors.neutral100
    )
}

/** Shared ellipsis-at-end overflow used across single-line titles. */
val TitleOverflow = TextOverflow.Ellipsis
