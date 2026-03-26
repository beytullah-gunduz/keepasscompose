package org.github.keepasscompose

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.github.keepasscompose.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KeePass Compose",
            icon = painterResource("icon.png"),
        ) {
            App()
        }
    }
}
