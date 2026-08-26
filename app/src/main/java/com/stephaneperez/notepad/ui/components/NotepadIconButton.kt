package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.ui.theme.NotepadColors

/**
 * 44x44dp icon button. Rest: transparent. Hover/focus: accent @ 12%. Pressed: accent @ 22%.
 * Corner radius 4dp. Focus ring: 2dp accent, 2dp offset (approximated with a border here
 * since Compose has no native ring-offset primitive).
 */
@Composable
fun NotepadIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()

    val background = when {
        pressed -> NotepadColors.accentPressed22
        hovered || focused -> NotepadColors.accentHover12
        else -> androidx.compose.ui.graphics.Color.Transparent
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .then(
                if (focused) Modifier.border(2.dp, NotepadColors.accent, RoundedCornerShape(4.dp))
                else Modifier
            )
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = NotepadColors.ink,
            modifier = Modifier.size(20.dp)
        )
    }
}
