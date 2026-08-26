package com.stephaneperez.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stephaneperez.notepad.ui.NotepadScreen
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.SimpleNotepadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleNotepadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NotepadColors.ground,
                ) {
                    NotepadScreen()
                }
            }
        }
    }
}
