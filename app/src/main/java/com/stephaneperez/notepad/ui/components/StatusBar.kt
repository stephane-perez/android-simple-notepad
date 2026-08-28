package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.R
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType

@Composable
fun NotepadStatusBar(
    filePath: String,
    lineCount: Int,
    charCount: Int,
    modifier: Modifier = Modifier,
) {
    val lines = pluralStringResource(R.plurals.status_lines, lineCount, lineCount)
    val chars = pluralStringResource(R.plurals.status_chars, charCount, charCount)

    Row(
        modifier = modifier
            .background(NotepadColors.surface)
            .topHairline(NotepadColors.divider)
            .padding(vertical = 7.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = filePath,
            style = NotepadType.path,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = "$lines · $chars",
            style = NotepadType.metaAndStatus,
        )
    }
}
