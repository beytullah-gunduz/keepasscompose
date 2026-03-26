package org.github.keepasscompose.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

actual class FilePicker actual constructor() {

    actual suspend fun pickFile(extensions: List<String>): String? = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, "Open Database", FileDialog.LOAD)
        dialog.filenameFilter = FilenameFilter { _, name ->
            extensions.isEmpty() || extensions.any { name.endsWith(".$it", ignoreCase = true) }
        }
        dialog.isVisible = true
        val directory = dialog.directory
        val file = dialog.file
        if (directory != null && file != null) "$directory$file" else null
    }

    actual suspend fun pickSaveLocation(defaultName: String, extensions: List<String>): String? =
        withContext(Dispatchers.IO) {
            val dialog = FileDialog(null as Frame?, "Save Database", FileDialog.SAVE)
            dialog.file = defaultName
            dialog.filenameFilter = FilenameFilter { _, name ->
                extensions.isEmpty() || extensions.any { name.endsWith(".$it", ignoreCase = true) }
            }
            dialog.isVisible = true
            val directory = dialog.directory
            val file = dialog.file
            if (directory != null && file != null) "$directory$file" else null
        }
}
