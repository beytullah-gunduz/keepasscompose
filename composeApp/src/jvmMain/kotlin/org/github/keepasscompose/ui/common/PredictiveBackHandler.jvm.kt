package org.github.keepasscompose.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun PredictiveBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
