package org.github.keepasscompose.ui.common

import androidx.compose.runtime.Composable

/**
 * Android predictive back gesture handler.
 *
 * Delegates to `androidx.activity.compose.BackHandler` which automatically
 * supports predictive back animations on Android 14+ when
 * `android:enableOnBackInvokedCallback="true"` is set in the manifest.
 *
 * The predictive back gesture provides a visual preview animation during
 * the swipe-back gesture, showing the user what they'll navigate to.
 */
@Composable
actual fun PredictiveBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
