package org.github.keepasscompose

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KeePass Compose",
        icon = painterResource("icon.png"),
    ) {
        App()
    }
}
