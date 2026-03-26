package org.github.keepasscompose.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No system back button on Desktop — handled by UI back arrows
}
