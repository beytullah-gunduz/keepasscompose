package org.github.keepasscompose.core.common

expect class FilePicker() {
    suspend fun pickFile(extensions: List<String>): String?
    suspend fun pickSaveLocation(defaultName: String, extensions: List<String>): String?
}
