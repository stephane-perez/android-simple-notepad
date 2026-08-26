package com.stephaneperez.notepad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType
import kotlinx.coroutines.delay

@Composable
fun NotepadToastHost(
    message: String,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            delay(2200)
            onDismissed()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = message.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NotepadColors.neutral900)
                    .shadow(3.dp, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text = message, style = NotepadType.toast)
            }
        }
    }
}
