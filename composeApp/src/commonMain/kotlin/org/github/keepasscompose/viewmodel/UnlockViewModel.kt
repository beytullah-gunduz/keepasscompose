package org.github.keepasscompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.database.KdbxReader
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.KdbxDatabase

sealed interface UnlockState {
    data object Idle : UnlockState
    data object Loading : UnlockState
    data class Success(val database: KdbxDatabase) : UnlockState
    data class Error(val message: String) : UnlockState
}

class UnlockViewModel(
    private val kdbxReader: KdbxReader,
    private val fileSystem: FileSystem,
) : ViewModel() {

    private val _state = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    private val _keyFilePath = MutableStateFlow("")
    val keyFilePath: StateFlow<String> = _keyFilePath.asStateFlow()

    fun setKeyFilePath(path: String) {
        _keyFilePath.value = path
    }

    fun clearKeyFile() {
        _keyFilePath.value = ""
    }

    fun unlock(databasePath: String, password: String) {
        if (password.isBlank()) {
            _state.value = UnlockState.Error("Password cannot be empty")
            return
        }
        if (!fileSystem.fileExists(databasePath)) {
            _state.value = UnlockState.Error("Database file not found")
            return
        }

        _state.value = UnlockState.Loading

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val data = fileSystem.readFile(databasePath)
                val keyFileData = _keyFilePath.value.let { path ->
                    if (path.isNotEmpty() && fileSystem.fileExists(path)) {
                        fileSystem.readFile(path)
                    } else {
                        null
                    }
                }
                val compositeKey = CompositeKey(
                    password = password,
                    keyFileData = keyFileData,
                )
                val database = kdbxReader.readDatabase(data, compositeKey)
                _state.value = UnlockState.Success(database)
            } catch (e: Exception) {
                _state.value = UnlockState.Error(
                    e.message ?: "Failed to unlock database",
                )
            }
        }
    }

    fun resetState() {
        _state.value = UnlockState.Idle
    }
}
