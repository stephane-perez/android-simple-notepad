package com.stephaneperez.notepad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The design is light-only ("warm near-white ground") — there is no dark variant in the
 * handoff, so the app intentionally does not offer one.
 */
private val NotepadColorScheme = lightColorScheme(
    primary = NotepadColors.accent,
    onPrimary = NotepadColors.surface,
    background = NotepadColors.ground,
    onBackground = NotepadColors.ink,
    surface = NotepadColors.surface,
    onSurface = NotepadColors.ink,
    outline = NotepadColors.divider,
    surfaceVariant = NotepadColors.neutral100,
    onSurfaceVariant = NotepadColors.inkMeta,
)

@Composable
fun SimpleNotepadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NotepadColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
