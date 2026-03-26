package org.github.keepasscompose.core.common

import java.io.File

actual fun dataStoreFilePath(): String = File(System.getProperty("user.home"), ".keepasscompose${File.separator}$DATA_STORE_FILE_NAME").absolutePath
