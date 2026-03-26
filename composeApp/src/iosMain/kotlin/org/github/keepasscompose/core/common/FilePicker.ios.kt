package org.github.keepasscompose.core.common

actual class FilePicker actual constructor() {

    actual suspend fun pickFile(extensions: List<String>): String? {
        // TODO: Wire to UIDocumentPickerViewController
        return null
    }

    actual suspend fun pickSaveLocation(defaultName: String, extensions: List<String>): String? {
        // TODO: Wire to UIDocumentPickerViewController in export mode
        return null
    }
}
