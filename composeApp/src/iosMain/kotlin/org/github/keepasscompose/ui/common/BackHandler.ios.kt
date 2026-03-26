package org.github.keepasscompose.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses swipe-to-go-back gesture handled by the navigation controller
}
