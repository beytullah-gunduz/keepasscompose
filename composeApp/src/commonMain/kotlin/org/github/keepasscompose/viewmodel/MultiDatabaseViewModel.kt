package org.github.keepasscompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.common.InactivityTimer
import org.github.keepasscompose.core.database.KdbxWriter
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.KdbxDatabase

data class DatabaseInstance(
    val id: String,
    val name: String,
    val filePath: String,
    val database: KdbxDatabase? = null,
    val compositeKey: CompositeKey? = null,
    val isDirty: Boolean = false,
    val isLocked: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
)

class MultiDatabaseViewModel(
    private val kdbxWriter: KdbxWriter,
    private val fileSystem: FileSystem,
) : ViewModel() {

    private val _instances = MutableStateFlow<List<DatabaseInstance>>(emptyList())
    val instances: StateFlow<List<DatabaseInstance>> = _instances.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    val inactivityTimer = InactivityTimer(viewModelScope)

    init {
        viewModelScope.launch {
            inactivityTimer.isExpired.collect { expired ->
                if (expired) lockActive()
            }
        }
    }

    val activeInstance: DatabaseInstance?
        get() = _instances.value.find { it.id == _activeId.value }

    fun openDatabase(
        id: String,
        name: String,
        filePath: String,
        database: KdbxDatabase,
        compositeKey: CompositeKey,
    ) {
        val existing = _instances.value.find { it.filePath == filePath }
        if (existing != null) {
            _activeId.value = existing.id
            return
        }

        val instance = DatabaseInstance(
            id = id,
            name = name,
            filePath = filePath,
            database = database,
            compositeKey = compositeKey,
        )
        _instances.update { it + instance }
        _activeId.value = id
    }

    fun switchTo(id: String) {
        if (_instances.value.any { it.id == id }) {
            _activeId.value = id
        }
    }

    fun markDirty(id: String) {
        updateInstance(id) { it.copy(isDirty = true) }
    }

    fun save(id: String) {
        val instance = _instances.value.find { it.id == id } ?: return
        val db = instance.database ?: return
        val key = instance.compositeKey ?: return

        updateInstance(id) { it.copy(saveState = SaveState.Saving) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (fileSystem.fileExists(instance.filePath)) {
                    val existingData = fileSystem.readFile(instance.filePath)
                    fileSystem.writeFile("${instance.filePath}.bak", existingData)
                }
                val data = kdbxWriter.writeDatabase(db, key)
                fileSystem.writeFile(instance.filePath, data)
                updateInstance(id) { it.copy(isDirty = false, saveState = SaveState.Success) }
            } catch (e: Exception) {
                updateInstance(id) {
                    it.copy(saveState = SaveState.Error(e.message ?: "Failed to save"))
                }
            }
        }
    }

    fun lock(id: String) {
        updateInstance(id) {
            it.copy(database = null, compositeKey = null, isLocked = true, isDirty = false)
        }
    }

    fun lockActive() {
        val id = _activeId.value ?: return
        lock(id)
        inactivityTimer.stop()
    }

    fun unlock(id: String, database: KdbxDatabase, compositeKey: CompositeKey) {
        updateInstance(id) {
            it.copy(database = database, compositeKey = compositeKey, isLocked = false)
        }
    }

    fun close(id: String) {
        _instances.update { list -> list.filter { it.id != id } }
        if (_activeId.value == id) {
            _activeId.value = _instances.value.firstOrNull()?.id
        }
    }

    fun needsSaveBeforeClose(id: String): Boolean =
        _instances.value.find { it.id == id }?.isDirty == true

    fun onUserActivity() {
        inactivityTimer.onUserActivity()
    }

    private fun updateInstance(id: String, transform: (DatabaseInstance) -> DatabaseInstance) {
        _instances.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }
}
