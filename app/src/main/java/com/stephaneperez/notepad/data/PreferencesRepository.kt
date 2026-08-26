package com.stephaneperez.notepad.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "notepad_prefs")

/**
 * `wrap` and `lineNumbers` persist across launches (DataStore), per the handoff's state
 * management section. The last-open document URI is also persisted here so the buffer
 * can be restored on relaunch; the buffer text itself is re-read from that URI via SAF
 * rather than duplicated into preferences.
 */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val WRAP = booleanPreferencesKey("wrap")
        val LINE_NUMBERS = booleanPreferencesKey("line_numbers")
        val LAST_URI = stringPreferencesKey("last_uri")
    }

    val wrapFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.WRAP] ?: false }
    val lineNumbersFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.LINE_NUMBERS] ?: true }
    val lastUriFlow: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_URI] }

    suspend fun currentWrap(): Boolean = wrapFlow.first()
    suspend fun currentLineNumbers(): Boolean = lineNumbersFlow.first()
    suspend fun currentLastUri(): String? = lastUriFlow.first()

    suspend fun setWrap(value: Boolean) {
        context.dataStore.edit { it[Keys.WRAP] = value }
    }

    suspend fun setLineNumbers(value: Boolean) {
        context.dataStore.edit { it[Keys.LINE_NUMBERS] = value }
    }

    suspend fun setLastUri(uri: String?) {
        context.dataStore.edit {
            if (uri == null) it.remove(Keys.LAST_URI) else it[Keys.LAST_URI] = uri
        }
    }
}
