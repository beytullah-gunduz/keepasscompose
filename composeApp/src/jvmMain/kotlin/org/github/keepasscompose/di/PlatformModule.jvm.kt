package org.github.keepasscompose.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Desktop-specific dependencies will be registered here
}
