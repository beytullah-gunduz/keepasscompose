package org.github.keepasscompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.common.InactivityTimer
import org.github.keepasscompose.core.database.KdbxWriter
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.KdbxDatabase

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data class Error(val message: String) : SaveState
}

class DatabaseViewModel(
    private val kdbxWriter: KdbxWriter,
    private val fileSystem: FileSystem,
) : ViewModel() {

    private val _database = MutableStateFlow<KdbxDatabase?>(null)
    val database: StateFlow<KdbxDatabase?> = _database.asStateFlow()

    private val _filePath = MutableStateFlow<String?>(null)
    val filePath: StateFlow<String?> = _filePath.asStateFlow()

    private val _compositeKey = MutableStateFlow<CompositeKey?>(null)

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    val inactivityTimer = InactivityTimer(viewModelScope)

    init {
        viewModelScope.launch {
            inactivityTimer.isExpired.collect { expired ->
                if (expired) lock()
            }
        }
    }

    fun setDatabase(database: KdbxDatabase, filePath: String, compositeKey: CompositeKey) {
        _database.value = database
        _filePath.value = filePath
        _compositeKey.value = compositeKey
        _isDirty.value = false
        _isLocked.value = false
    }

    fun markDirty() {
        _isDirty.value = true
    }

    fun save() {
        val db = _database.value ?: return
        val path = _filePath.value ?: return
        val key = _compositeKey.value ?: return
        performSave(db, path, key)
    }

    fun saveAs(newPath: String) {
        val db = _database.value ?: return
        val key = _compositeKey.value ?: return
        performSave(db, newPath, key)
        _filePath.value = newPath
    }

    private fun performSave(db: KdbxDatabase, path: String, key: CompositeKey) {
        _saveState.value = SaveState.Saving

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Create backup before overwrite
                if (fileSystem.fileExists(path)) {
                    val backupPath = "$path.bak"
                    val existingData = fileSystem.readFile(path)
                    fileSystem.writeFile(backupPath, existingData)
                }

                val data = kdbxWriter.writeDatabase(db, key)
                fileSystem.writeFile(path, data)
                _isDirty.value = false
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(
                    e.message ?: "Failed to save database",
                )
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun needsSaveBeforeClose(): Boolean = _isDirty.value

    fun lock() {
        // Clear sensitive data but preserve file path for re-unlock
        _database.value = null
        _compositeKey.value = null
        _isLocked.value = true
        _isDirty.value = false
        inactivityTimer.stop()
    }

    fun close() {
        _database.value = null
        _filePath.value = null
        _compositeKey.value = null
        _isDirty.value = false
        _isLocked.value = false
        _saveState.value = SaveState.Idle
        inactivityTimer.stop()
    }

    fun onUserActivity() {
        inactivityTimer.onUserActivity()
    }
}
