package org.github.keepasscompose.core.common

actual class PlatformFileSystem actual constructor() : BaseFileSystem() {
    override fun getDefaultDatabaseDirectory(): String = AndroidContextHolder.applicationContext.filesDir
        .resolve("databases")
        .absolutePath
}
