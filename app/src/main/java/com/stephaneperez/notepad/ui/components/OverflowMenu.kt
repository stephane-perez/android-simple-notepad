package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.R
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType

@Composable
fun NotepadOverflowMenu(
    expanded: Boolean,
    wrap: Boolean,
    lineNumbers: Boolean,
    onDismiss: () -> Unit,
    onToggleWrap: () -> Unit,
    onToggleLineNumbers: () -> Unit,
) {
    val on = stringResource(R.string.state_on)
    val off = stringResource(R.string.state_off)
    val wrapIsOn = stringResource(R.string.state_wrap_is_on)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = androidx.compose.ui.unit.DpOffset((-9).dp, 0.dp),
        modifier = androidx.compose.ui.Modifier
            .width(224.dp)
            .background(NotepadColors.surface)
            .border(1.dp, NotepadColors.divider, RoundedCornerShape(4.dp))
            .shadow(3.dp, RoundedCornerShape(4.dp)),
    ) {
        MenuRow(
            label = stringResource(R.string.menu_word_wrap),
            value = if (wrap) on else off,
            onClick = onToggleWrap,
        )
        androidx.compose.material3.HorizontalDivider(color = NotepadColors.divider, thickness = 1.dp)
        MenuRow(
            label = stringResource(R.string.menu_line_numbers),
            value = if (wrap) wrapIsOn else if (lineNumbers) on else off,
            onClick = onToggleLineNumbers,
            inert = wrap,
        )
    }
}

@Composable
private fun MenuRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    inert: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !inert, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = NotepadType.menuLabel)
        Text(
            text = value.uppercase(),
            style = NotepadType.microLabel,
            modifier = if (inert) Modifier.alpha(0.7f) else Modifier,
        )
    }
}
