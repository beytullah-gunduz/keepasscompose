package org.github.keepasscompose.di

import org.github.keepasscompose.core.common.FileSystem
import org.github.keepasscompose.core.common.PlatformFileSystem
import org.koin.dsl.module

val appModule = module {
    single<FileSystem> { PlatformFileSystem() }
}
