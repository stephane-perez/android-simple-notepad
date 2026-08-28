package com.stephaneperez.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stephaneperez.notepad.R
import com.stephaneperez.notepad.data.LocalFileEntry
import com.stephaneperez.notepad.ui.theme.NotepadColors
import com.stephaneperez.notepad.ui.theme.NotepadType

@Composable
fun OpenDocumentOverlay(
    breadcrumb: String,
    files: List<LocalFileEntry>,
    onBack: () -> Unit,
    onFileSelected: (LocalFileEntry) -> Unit,
    onBrowseSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NotepadColors.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bottomHairline(NotepadColors.divider)
                .padding(start = 6.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotepadIconButton(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
            )
            Text(
                text = stringResource(R.string.open_document_title),
                style = NotepadType.overlayTitle,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        // Path breadcrumb row
        Text(
            text = breadcrumb,
            style = NotepadType.path.copy(color = NotepadColors.inkPathBreadcrumb),
            modifier = Modifier
                .fillMaxWidth()
                .bottomHairline(NotepadColors.divider)
                .padding(vertical = 14.dp, horizontal = 18.dp),
        )

        // File rows
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files, key = { it.uri.toString() }) { entry ->
                FileRow(entry = entry, onClick = { onFileSelected(entry) })
            }
            item {
                // Fallback: launch the platform document picker for anything outside
                // the app's own documents directory.
                FileRow(
                    entry = null,
                    label = stringResource(R.string.browse_device_storage),
                    onClick = onBrowseSystem,
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    entry: LocalFileEntry?,
    onClick: () -> Unit,
    label: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        pressed -> NotepadColors.accentRowPressed14
        hovered -> NotepadColors.inkHover4
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .bottomHairline(NotepadColors.divider)
            .padding(vertical = 14.dp, horizontal = 18.dp),
    ) {
        Text(
            text = label ?: entry?.name.orEmpty(),
            style = NotepadType.fieldOrFilename,
        )
        if (entry != null) {
            Text(
                text = stringResource(R.string.file_meta, entry.sizeBytes, entry.modified),
                style = NotepadType.metaAndStatus.copy(color = NotepadColors.inkMeta),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
