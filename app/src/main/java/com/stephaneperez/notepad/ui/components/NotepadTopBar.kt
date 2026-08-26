package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.R
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType

@Composable
fun NotepadTopBar(
    filename: String,
    dirty: Boolean,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onFind: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .background(NotepadColors.surface)
            .bottomHairline(NotepadColors.divider)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = filename,
                style = NotepadType.appBarTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (dirty) {
                Box(
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .size(6.dp)
                        .background(NotepadColors.accent, CircleShape)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_file_plus),
                contentDescription = "New",
                onClick = onNew,
            )
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_folder_open),
                contentDescription = "Open",
                onClick = onOpen,
            )
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_save),
                contentDescription = "Save",
                onClick = onSave,
            )
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Find",
                onClick = onFind,
            )
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = "More",
                onClick = onMore,
            )
        }
    }
}
