package org.github.keepasscompose

import androidx.compose.ui.window.ComposeUIViewController
import org.github.keepasscompose.di.initKoin
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
