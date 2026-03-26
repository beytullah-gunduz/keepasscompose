package org.github.keepasscompose.core.common

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class PlatformFileSystem actual constructor() : BaseFileSystem() {
    override fun getDefaultDatabaseDirectory(): String {
        val paths =
            NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true,
            )
        val documentsDir = paths.first() as String
        return "$documentsDir/databases"
    }
}
