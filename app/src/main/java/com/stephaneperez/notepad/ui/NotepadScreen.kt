package com.stephaneperez.notepad.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stephaneperez.notepad.data.NotepadViewModel
import com.stephaneperez.notepad.data.PendingAction
import com.stephaneperez.notepad.ui.components.EditorArea
import com.stephaneperez.notepad.ui.components.FindReplaceStrip
import com.stephaneperez.notepad.ui.components.NotepadOverflowMenu
import com.stephaneperez.notepad.ui.components.NotepadStatusBar
import com.stephaneperez.notepad.ui.components.NotepadToastHost
import com.stephaneperez.notepad.ui.components.NotepadTopBar
import com.stephaneperez.notepad.ui.components.OpenDocumentOverlay
import com.stephaneperez.notepad.ui.components.UnsavedChangesDialog
import com.stephaneperez.notepad.ui.theme.NotepadColors

@Composable
fun NotepadScreen(viewModel: NotepadViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val files by viewModel.fileList.collectAsState()

    // ACTION_OPEN_DOCUMENT — used both by the in-app "Browse device storage" row and as
    // the platform adaptation the README calls out for a full system picker.
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.takePersistablePermission(uri)
            viewModel.openDocument(uri)
        }
    }

    // ACTION_CREATE_DOCUMENT — first Save for a never-saved buffer.
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.completeSaveAs(uri)
        }
    }

    fun handleSave() {
        if (state.uri == null) {
            createDocumentLauncher.launch(state.filename)
        } else {
            viewModel.save()
        }
    }

    // Back behavior: overflow menu -> find strip -> Open overlay -> dialog (cancel) -> system back,
    // which itself must run the unsaved-changes check before exit. We approximate the final
    // "exit" step by simply allowing the system back to proceed once nothing above is open,
    // deferring to New's own dirty-check semantics is out of scope for a bare back-to-exit.
    BackHandler(enabled = true) {
        viewModel.onBackPressed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NotepadColors.ground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotepadTopBar(
                filename = state.filename,
                dirty = state.dirty,
                onNew = viewModel::requestNew,
                onOpen = viewModel::requestOpen,
                onSave = { handleSave() },
                onFind = viewModel::toggleFind,
                onMore = viewModel::toggleMenu,
            )

            if (state.finding) {
                FindReplaceStrip(
                    query = state.query,
                    onQueryChanged = viewModel::onQueryChanged,
                    matchLabel = state.matchLabel,
                    replacement = state.replacement,
                    onReplacementChanged = viewModel::onReplacementChanged,
                    onReplaceAll = viewModel::replaceAll,
                    onPrevious = viewModel::previousMatch,
                    onNext = viewModel::nextMatch,
                    onDone = viewModel::closeFind,
                    hasMatches = state.totalMatches > 0,
                )
            }

            EditorArea(
                text = state.text,
                onTextChanged = viewModel::onTextChanged,
                wrap = state.wrap,
                gutterVisible = state.gutterVisible,
                lineCount = state.lineCount,
                modifier = Modifier.weight(1f),
            )

            NotepadStatusBar(
                filePath = state.filePath,
                counts = state.counts,
            )
        }

        // Overflow menu, anchored below the More button (top-right).
        Box(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)) {
            NotepadOverflowMenu(
                expanded = state.menuOpen,
                wrap = state.wrap,
                lineNumbers = state.lineNumbers,
                onDismiss = viewModel::dismissMenu,
                onToggleWrap = viewModel::toggleWrap,
                onToggleLineNumbers = viewModel::toggleLineNumbers,
            )
        }

        if (state.browsing) {
            OpenDocumentOverlay(
                breadcrumb = "/Documents/notes",
                files = files,
                onBack = viewModel::dismissBrowser,
                onFileSelected = { entry -> viewModel.openDocument(entry.uri) },
                onBrowseSystem = { openDocumentLauncher.launch(arrayOf("text/plain")) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        state.confirm?.let { pending ->
            UnsavedChangesDialog(
                pendingAction = pending,
                filename = state.filename,
                onCancel = viewModel::onDialogCancel,
                onDiscard = viewModel::onDialogDiscard,
                onSaveFirst = {
                    // Edge case: if the dirty buffer has never been saved (uri == null),
                    // "Save first" needs the CreateDocument picker to resolve a destination
                    // before the pending action can safely proceed. Route through Save,
                    // then let the person retry New/Open once the file exists.
                    if (state.uri == null) {
                        createDocumentLauncher.launch(state.filename)
                        viewModel.onDialogCancel() // dialog closes; buffer saves via the picker callback
                    } else {
                        viewModel.onDialogSaveFirst()
                    }
                },
            )
        }

        NotepadToastHost(
            message = state.toast,
            onDismissed = viewModel::consumeToast,
        )
    }
}
