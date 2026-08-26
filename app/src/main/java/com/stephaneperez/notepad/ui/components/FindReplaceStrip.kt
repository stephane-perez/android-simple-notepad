package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.R
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType

@Composable
fun FindReplaceStrip(
    query: String,
    onQueryChanged: (String) -> Unit,
    matchLabel: String,
    replacement: String,
    onReplacementChanged: (String) -> Unit,
    onReplaceAll: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    hasMatches: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NotepadColors.neutral100)
            .bottomHairline(NotepadColors.divider)
            .padding(top = 9.dp, start = 14.dp, end = 14.dp, bottom = 14.dp),
    ) {
        // Row 1: Find field + match counter
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotepadTextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = "Find",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.widthIn(min = 9.dp))
            Text(
                text = matchLabel,
                style = NotepadType.counter,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 52.dp),
            )
        }

        Spacer(Modifier.padding(top = 4.5.dp))

        // Row 2: Replace-with field + Replace all
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotepadTextField(
                value = replacement,
                onValueChange = onReplacementChanged,
                placeholder = "Replace with",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.widthIn(min = 9.dp))
            OutlinedSecondaryButton(text = "Replace all", onClick = onReplaceAll)
        }

        Spacer(Modifier.padding(top = 4.5.dp))

        // Row 3: prev / next, spacer, Done
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_chevron_up),
                contentDescription = "Previous match",
                onClick = onPrevious,
                enabled = hasMatches,
            )
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = "Next match",
                onClick = onNext,
                enabled = hasMatches,
            )
            Spacer(Modifier.weight(1f))
            GhostButton(text = "Done", onClick = onDone)
        }
    }
}

@Composable
private fun NotepadTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        focused -> NotepadColors.accent
        hovered -> NotepadColors.inkBorder45
        else -> NotepadColors.divider
    }
    val borderWidth = if (focused) 2.dp else 1.dp

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = NotepadType.fieldOrFilename.copy(color = NotepadColors.inkMeta)) },
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = NotepadType.fieldOrFilename,
        keyboardOptions = KeyboardOptions.Default,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = NotepadColors.surface,
            unfocusedContainerColor = NotepadColors.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = NotepadColors.accent,
        ),
    )
}

@Composable
private fun OutlinedSecondaryButton(text: String, onClick: () -> Unit) {
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

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
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
            .background(bg),
    ) {
        Text(text, style = NotepadType.button)
    }
}
