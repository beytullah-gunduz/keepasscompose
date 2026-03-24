package org.github.keepasscompose.ui.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.github.keepasscompose.ui.screens.UnlockScreen

@Composable
fun AppNavigator() {
    Navigator(UnlockScreen()) { navigator ->
        SlideTransition(navigator)
    }
}
