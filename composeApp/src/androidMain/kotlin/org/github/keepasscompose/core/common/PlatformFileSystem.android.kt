package org.github.keepasscompose.core.common

import android.net.Uri
import okio.FileSystem as OkioFileSystem

actual class PlatformFileSystem actual constructor() : BaseFileSystem() {
    override val okioFs: OkioFileSystem = OkioFileSystem.SYSTEM
    override fun getDefaultDatabaseDirectory(): String = AndroidContextHolder.applicationContext.filesDir
        .resolve("databases")
        .absolutePath

    override fun readFile(path: String): ByteArray {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            return AndroidContextHolder.applicationContext.contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot open content URI: $path")
        }
        return super.readFile(path)
    }

    override fun writeFile(path: String, data: ByteArray) {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            AndroidContextHolder.applicationContext.contentResolver
                .openOutputStream(uri, "wt")
                ?.use { it.write(data) }
                ?: throw IllegalStateException("Cannot write to content URI: $path")
            return
        }
        super.writeFile(path, data)
    }

    override fun fileExists(path: String): Boolean {
        if (path.startsWith("content://")) {
            return try {
                AndroidContextHolder.applicationContext.contentResolver
                    .openInputStream(Uri.parse(path))
                    ?.close()
                true
            } catch (_: Exception) {
                false
            }
        }
        return super.fileExists(path)
    }
}
