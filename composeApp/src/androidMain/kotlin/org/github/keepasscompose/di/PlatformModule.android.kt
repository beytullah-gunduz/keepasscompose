package org.github.keepasscompose.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Android-specific dependencies will be registered here
}
