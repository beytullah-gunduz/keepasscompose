package org.github.keepasscompose.core.common

import okio.Path.Companion.toPath
import okio.FileSystem as OkioFileSystem

interface FileSystem {
    fun getDefaultDatabaseDirectory(): String

    fun readFile(path: String): ByteArray

    fun writeFile(path: String, data: ByteArray)

    fun fileExists(path: String): Boolean

    fun listFiles(directory: String): List<String>
}

abstract class BaseFileSystem : FileSystem {
    protected val okioFs: OkioFileSystem = OkioFileSystem.SYSTEM

    override fun readFile(path: String): ByteArray = okioFs.read(path.toPath()) { readByteArray() }

    override fun writeFile(path: String, data: ByteArray) {
        val filePath = path.toPath()
        filePath.parent?.let { okioFs.createDirectories(it) }
        okioFs.write(filePath) { write(data) }
    }

    override fun fileExists(path: String): Boolean = okioFs.exists(path.toPath())

    override fun listFiles(directory: String): List<String> {
        val dirPath = directory.toPath()
        if (!okioFs.exists(dirPath)) return emptyList()
        return okioFs.list(dirPath).map { it.toString() }
    }
}

expect class PlatformFileSystem() : BaseFileSystem
