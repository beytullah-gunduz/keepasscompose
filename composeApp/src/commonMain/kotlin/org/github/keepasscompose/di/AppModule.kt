package org.github.keepasscompose.di

import org.github.keepasscompose.core.common.AppSettings
import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.common.PlatformFileSystem
import org.github.keepasscompose.core.common.createDataStore
import org.github.keepasscompose.core.common.dataStoreFilePath
import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.database.KdbxReader
import org.github.keepasscompose.core.database.KdbxWriter
import org.github.keepasscompose.viewmodel.DatabaseViewModel
import org.github.keepasscompose.viewmodel.GroupNavigationViewModel
import org.github.keepasscompose.viewmodel.UnlockViewModel
import org.koin.dsl.module

val appModule =
    module {
        single<FileSystem> { PlatformFileSystem() }
        single { createDataStore { dataStoreFilePath() } }
        single { AppSettings(get()) }

        // Crypto & database
        single { PlatformCryptoProvider() }
        single { KdbxReader(get<PlatformCryptoProvider>()) }
        single { KdbxWriter(get<PlatformCryptoProvider>()) }

        // ViewModels
        single { UnlockViewModel(get(), get()) }
        single { DatabaseViewModel(get(), get()) }
        single { GroupNavigationViewModel() }
    }
