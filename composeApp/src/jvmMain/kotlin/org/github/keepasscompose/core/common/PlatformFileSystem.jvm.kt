package org.github.keepasscompose.core.common

import java.io.File
import okio.FileSystem as OkioFileSystem

actual class PlatformFileSystem actual constructor() : BaseFileSystem() {
    override val okioFs: OkioFileSystem = OkioFileSystem.SYSTEM
    override fun getDefaultDatabaseDirectory(): String {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".keepasscompose${File.separator}databases").absolutePath
    }
}
