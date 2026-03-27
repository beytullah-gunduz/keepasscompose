package org.github.keepasscompose.ui.common

import androidx.compose.runtime.Composable

/**
 * Platform-abstracted predictive back gesture handler.
 *
 * On Android 14+ (API 34+), this enables predictive back gesture animations
 * where the user can see a preview of the previous screen while swiping back.
 * On other platforms, this delegates to the standard [BackHandler].
 *
 * @param enabled Whether the back handler is enabled.
 * @param onBack Called when the back gesture is completed.
 */
@Composable
expect fun PredictiveBackHandler(enabled: Boolean = true, onBack: () -> Unit)
