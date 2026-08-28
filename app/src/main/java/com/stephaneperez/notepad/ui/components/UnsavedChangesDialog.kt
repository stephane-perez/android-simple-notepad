package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stephaneperez.notepad.R
import com.stephaneperez.notepad.data.PendingAction
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType

@Composable
fun UnsavedChangesDialog(
    pendingAction: PendingAction,
    filename: String,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onSaveFirst: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(NotepadColors.surface)
                .border(1.dp, NotepadColors.divider, RoundedCornerShape(7.dp))
                .shadow(12.dp, RoundedCornerShape(7.dp))
                .padding(18.dp),
        ) {
            Text(text = stringResource(R.string.dialog_title), style = NotepadType.dialogTitle)

            // Two full sentence templates (one per pending action) rather than composing
            // a generic template with an inserted phrase — safer to translate correctly
            // across languages with different sentence structures.
            val bodyRes = if (pendingAction == PendingAction.NEW) R.string.dialog_body_new else R.string.dialog_body_open
            Text(
                text = stringResource(bodyRes, filename),
                style = NotepadType.body,
                modifier = Modifier.padding(top = 14.dp),
            )

            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.End),
            ) {
                DialogGhostButton(text = stringResource(R.string.action_cancel), onClick = onCancel)
                DialogOutlinedButton(text = stringResource(R.string.action_discard), onClick = onDiscard)
                DialogAccentOutlinedButton(text = stringResource(R.string.action_save_first), onClick = onSaveFirst)
            }
        }
    }
}

@Composable
private fun DialogGhostButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        pressed -> NotepadColors.inkPressed14
        hovered -> NotepadColors.inkHover7
        else -> Color.Transparent
    }
    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg),
    ) {
        Text(text, style = NotepadType.button)
    }
}

@Composable
private fun DialogOutlinedButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        pressed -> NotepadColors.inkPressed14
        hovered -> NotepadColors.inkHover7
        else -> Color.Transparent
    }
    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, NotepadColors.divider, RoundedCornerShape(4.dp))
            .background(bg),
    ) {
        Text(text, style = NotepadType.button)
    }
}

/** The primary action is an outline, never a filled block — outlined in accent color. */
@Composable
private fun DialogAccentOutlinedButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        pressed -> NotepadColors.accentPressed22
        hovered -> NotepadColors.accentHover12
        else -> Color.Transparent
    }
    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, NotepadColors.accent, RoundedCornerShape(4.dp))
            .background(bg),
    ) {
        Text(text, style = NotepadType.button.copy(color = NotepadColors.accent))
    }
}
