package com.stephaneperez.notepad.data

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/** A file entry surfaced by the in-app "Open document" list (see README §Platform adaptations). */
data class LocalFileEntry(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val modified: String,
)

class NotepadViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(NotepadUiState())
    val uiState: StateFlow<NotepadUiState> = _uiState.asStateFlow()

    private val _fileList = MutableStateFlow<List<LocalFileEntry>>(emptyList())
    val fileList: StateFlow<List<LocalFileEntry>> = _fileList.asStateFlow()

    init {
        viewModelScope.launch {
            val wrap = prefs.currentWrap()
            val lineNumbers = prefs.currentLineNumbers()
            _uiState.update { it.copy(wrap = wrap, lineNumbers = lineNumbers) }

            prefs.currentLastUri()?.let { uriString ->
                runCatching { Uri.parse(uriString) }.getOrNull()?.let { uri ->
                    loadDocument(uri)
                }
            }
        }
    }

    // ---- Text editing --------------------------------------------------

    fun onTextChanged(newText: String) {
        _uiState.update { it.copy(text = newText, dirty = true) }
    }

    // ---- New / Open / Save --------------------------------------------

    /** Called by the "New" app-bar action. */
    fun requestNew() {
        if (_uiState.value.dirty) {
            _uiState.update { it.copy(confirm = PendingAction.NEW) }
        } else {
            performNew()
        }
    }

    /** Called by the "Open" app-bar action. */
    fun requestOpen() {
        if (_uiState.value.dirty) {
            _uiState.update { it.copy(confirm = PendingAction.OPEN) }
        } else {
            openBrowser()
        }
    }

    private fun performNew() {
        _uiState.update {
            NotepadUiState(
                wrap = it.wrap,
                lineNumbers = it.lineNumbers,
            )
        }
        viewModelScope.launch { prefs.setLastUri(null) }
    }

    private fun openBrowser() {
        loadFileList()
        _uiState.update { it.copy(browsing = true, menuOpen = false) }
    }

    fun dismissBrowser() {
        _uiState.update { it.copy(browsing = false) }
    }

    /** Confirmation dialog actions. */
    fun onDialogCancel() {
        _uiState.update { it.copy(confirm = null) }
    }

    fun onDialogDiscard() {
        val pending = _uiState.value.confirm
        _uiState.update { it.copy(confirm = null) }
        when (pending) {
            PendingAction.NEW -> performNew()
            PendingAction.OPEN -> openBrowser()
            null -> Unit
        }
    }

    fun onDialogSaveFirst() {
        val pending = _uiState.value.confirm
        save()
        _uiState.update { it.copy(confirm = null) }
        when (pending) {
            PendingAction.NEW -> performNew()
            PendingAction.OPEN -> openBrowser()
            null -> Unit
        }
    }

    /** User picked a row in the Open-document overlay, or a document from the system picker. */
    fun openDocument(uri: Uri) {
        loadDocument(uri)
        _uiState.update {
            it.copy(browsing = false, finding = false, query = "", replacement = "")
        }
        viewModelScope.launch { prefs.setLastUri(uri.toString()) }
    }

    private fun loadDocument(uri: Uri) {
        val context = getApplication<Application>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }
        }.getOrNull()?.let { content ->
            val name = queryDisplayName(uri) ?: "untitled.txt"
            _uiState.update {
                it.copy(
                    filename = name,
                    uri = uri,
                    text = content,
                    dirty = false,
                    filePath = pathForDisplay(uri, name),
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val context = getApplication<Application>()
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        }.getOrNull()
    }

    private fun pathForDisplay(uri: Uri, name: String): String {
        // SAF URIs don't expose a real filesystem path; approximate the status-bar path
        // from the document tree/id when available, falling back to the filename alone.
        return runCatching {
            val docId = DocumentsContract.getDocumentId(uri)
            "/${docId.substringAfter(':', docId)}"
        }.getOrElse { "/$name" }
    }

    /** Save writes straight to the current file — no Save-as dialog, no confirmation step. */
    fun save() {
        val state = _uiState.value
        val uri = state.uri
        val context = getApplication<Application>()

        if (uri == null) {
            // Untitled buffer: caller (UI) must launch ACTION_CREATE_DOCUMENT and then
            // call completeSaveAs(uri) with the result.
            return
        }

        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(state.text.toByteArray(Charsets.UTF_8))
            }
        }.onSuccess {
            _uiState.update {
                it.copy(dirty = false, menuOpen = false, toast = "Saved  ${it.filePath}")
            }
        }
    }

    /** Called after the platform's ACTION_CREATE_DOCUMENT picker returns a destination. */
    fun completeSaveAs(uri: Uri) {
        val context = getApplication<Application>()
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val name = queryDisplayName(uri) ?: _uiState.value.filename
        _uiState.update { it.copy(uri = uri, filename = name, filePath = pathForDisplay(uri, name)) }
        viewModelScope.launch { prefs.setLastUri(uri.toString()) }
        save()
    }

    /** Take persistable permission right after ACTION_OPEN_DOCUMENT returns, before loading. */
    fun takePersistablePermission(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    // ---- Find & replace -------------------------------------------------

    fun toggleFind() {
        _uiState.update { it.copy(finding = !it.finding) }
    }

    fun closeFind() {
        _uiState.update { it.copy(finding = false) }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, matchIndex = 0) }
    }

    fun onReplacementChanged(replacement: String) {
        _uiState.update { it.copy(replacement = replacement) }
    }

    fun nextMatch() {
        _uiState.update {
            val total = it.totalMatches
            if (total == 0) it else it.copy(matchIndex = (it.matchIndex + 1) % total)
        }
    }

    fun previousMatch() {
        _uiState.update {
            val total = it.totalMatches
            if (total == 0) it else it.copy(matchIndex = (it.matchIndex - 1 + total) % total)
        }
    }

    /** Plain literal, case-sensitive substring replace-all. */
    fun replaceAll() {
        val state = _uiState.value
        if (state.query.isEmpty()) {
            _uiState.update { it.copy(toast = "No matches") }
            return
        }
        val occurrences = state.totalMatches
        if (occurrences == 0) {
            _uiState.update { it.copy(toast = "No matches") }
            return
        }
        val newText = state.text.replace(state.query, state.replacement)
        val label = if (occurrences == 1) "1 match replaced" else "$occurrences matches replaced"
        _uiState.update {
            it.copy(text = newText, dirty = true, matchIndex = 0, toast = label)
        }
    }

    // ---- Overflow menu ---------------------------------------------------

    fun toggleMenu() {
        _uiState.update { it.copy(menuOpen = !it.menuOpen) }
    }

    fun dismissMenu() {
        _uiState.update { it.copy(menuOpen = false) }
    }

    fun toggleWrap() {
        val newValue = !_uiState.value.wrap
        _uiState.update { it.copy(wrap = newValue) }
        viewModelScope.launch { prefs.setWrap(newValue) }
    }

    fun toggleLineNumbers() {
        if (_uiState.value.wrap) return // inert while wrap is on
        val newValue = !_uiState.value.lineNumbers
        _uiState.update { it.copy(lineNumbers = newValue) }
        viewModelScope.launch { prefs.setLineNumbers(newValue) }
    }

    // ---- Toast -----------------------------------------------------------

    fun consumeToast() {
        _uiState.update { it.copy(toast = "") }
    }

    // ---- Back navigation ---------------------------------------------------
    // Order: overflow menu -> find strip -> Open overlay -> dialog (cancel) -> system back.
    // Returns true if this call consumed the back press.
    fun onBackPressed(): Boolean {
        val state = _uiState.value
        return when {
            state.menuOpen -> { dismissMenu(); true }
            state.finding -> { closeFind(); true }
            state.browsing -> { dismissBrowser(); true }
            state.confirm != null -> { onDialogCancel(); true }
            else -> false
        }
    }

    // ---- In-app document list (stands in for the system picker; see README) -------

    private fun loadFileList() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val docsDir = context.getExternalFilesDir(null) ?: return@launch
            val entries = docsDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
                ?.sortedBy { it.name }
                ?.map { f ->
                    LocalFileEntry(
                        uri = Uri.fromFile(f),
                        name = f.name,
                        sizeBytes = f.length(),
                        modified = android.text.format.DateFormat.format("d MMM yyyy", f.lastModified()).toString(),
                    )
                } ?: emptyList()
            _fileList.value = entries
        }
    }
}
