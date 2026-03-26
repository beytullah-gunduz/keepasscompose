package org.github.keepasscompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import org.github.keepasscompose.core.common.AppSettings
import org.github.keepasscompose.ui.navigation.AppNavigator
import org.github.keepasscompose.ui.theme.KeePassTheme
import org.github.keepasscompose.ui.theme.resolveIsDarkTheme
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val appSettings: AppSettings = koinInject()
    val themePreference by appSettings.themePreference.collectAsState(
        initial = AppSettings.Defaults.THEME_PREFERENCE,
    )
    KeePassTheme(darkTheme = resolveIsDarkTheme(themePreference)) {
        AppNavigator()
    }
}
