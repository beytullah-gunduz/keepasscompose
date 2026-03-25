package org.github.keepasscompose.core.common

actual fun dataStoreFilePath(): String =
    AndroidContextHolder.applicationContext.filesDir
        .resolve("datastore")
        .resolve(DATA_STORE_FILE_NAME)
        .absolutePath
