package org.github.keepasscompose.core.common

import java.io.File

actual class PlatformFileSystem actual constructor() : BaseFileSystem() {
    override fun getDefaultDatabaseDirectory(): String {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".keepasscompose${File.separator}databases").absolutePath
    }
}
