package org.github.keepasscompose.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.ui.screens.EntryEditorField
import org.github.keepasscompose.ui.screens.EntryEditorResult
import kotlin.time.Clock

sealed interface EntryEditorState {
    data object Idle : EntryEditorState

    data class Editing(val entry: KdbxEntry) : EntryEditorState

    data class Saved(val entry: KdbxEntry) : EntryEditorState

    data class ValidationError(val message: String) : EntryEditorState
}

class EntryEditorViewModel : ViewModel() {
    private val _state = MutableStateFlow<EntryEditorState>(EntryEditorState.Idle)
    val state: StateFlow<EntryEditorState> = _state.asStateFlow()

    private var originalEntry: KdbxEntry? = null

    fun startEditing(entry: KdbxEntry) {
        originalEntry = entry
        _state.value = EntryEditorState.Editing(entry)
    }

    fun startCreating() {
        originalEntry = null
        _state.value = EntryEditorState.Idle
    }

    fun save(result: EntryEditorResult): KdbxEntry? {
        if (result.title.isBlank()) {
            _state.value = EntryEditorState.ValidationError("Title is required")
            return null
        }

        val now = Clock.System.now()

        val fields =
            buildList {
                add(KdbxEntryField(KdbxEntry.FIELD_TITLE, result.title))
                add(KdbxEntryField(KdbxEntry.FIELD_USER_NAME, result.userName))
                add(KdbxEntryField(KdbxEntry.FIELD_PASSWORD, result.password, isProtected = true))
                add(KdbxEntryField(KdbxEntry.FIELD_URL, result.url))
                add(KdbxEntryField(KdbxEntry.FIELD_NOTES, result.notes))
                result.customFields.forEach { cf ->
                    add(KdbxEntryField(cf.key, cf.value, cf.isProtected))
                }
            }

        val existingEntry = originalEntry
        val history =
            if (existingEntry != null) {
                existingEntry.history + existingEntry.copy(history = emptyList())
            } else {
                emptyList()
            }

        val entry =
            KdbxEntry(
                uuid = existingEntry?.uuid ?: generateUuid(),
                icon = KdbxIcon(standardIndex = result.iconIndex),
                tags = result.tags,
                fields = fields,
                history = history,
                creationTime = existingEntry?.creationTime ?: now,
                lastModificationTime = now,
                lastAccessTime = now,
                expiryTime = existingEntry?.expiryTime,
                expires = result.expires,
            )

        _state.value = EntryEditorState.Saved(entry)
        return entry
    }

    fun resetState() {
        _state.value = EntryEditorState.Idle
        originalEntry = null
    }

    private fun generateUuid(): String {
        val chars = "0123456789abcdef"
        return buildString(32) {
            repeat(32) { append(chars.random()) }
        }
    }
}
