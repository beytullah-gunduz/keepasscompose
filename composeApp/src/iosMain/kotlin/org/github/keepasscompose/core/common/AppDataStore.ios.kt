package org.github.keepasscompose.core.common

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun dataStoreFilePath(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    )
    val documentsDir = paths.first() as String
    return "$documentsDir/$DATA_STORE_FILE_NAME"
}
