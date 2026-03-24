package org.github.keepasscompose

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.github.keepasscompose.di.appModule
import org.github.keepasscompose.di.platformModule
import org.github.keepasscompose.ui.navigation.AppNavigator

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(appModule, platformModule())
    }) {
        MaterialTheme {
            AppNavigator()
        }
    }
}
