package org.github.keepasscompose.di

import org.github.keepasscompose.core.common.AppSettings
import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.common.PlatformFileSystem
import org.github.keepasscompose.core.common.createDataStore
import org.github.keepasscompose.core.common.dataStoreFilePath
import org.koin.dsl.module

val appModule =
    module {
        single<FileSystem> { PlatformFileSystem() }
        single { createDataStore { dataStoreFilePath() } }
        single { AppSettings(get()) }
    }
