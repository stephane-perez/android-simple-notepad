package com.stephaneperez.notepad.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.stephaneperez.notepad.R

/**
 * Two deliberate exceptions to the serif rule: editor text, filenames, paths and the
 * toast are monospace (hard product requirement, not user-changeable). Everything else
 * chrome-like is serif (Cormorant Garamond for headings, Lora for body/controls).
 */
val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal)
)

val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal)
)

// Platform monospace — used for the editor, filenames, paths and the toast.
val NotepadMono = FontFamily.Monospace
