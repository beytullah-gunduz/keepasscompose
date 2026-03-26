package org.github.keepasscompose.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.github.keepasscompose.ui.screens.RecentDatabase
import org.github.keepasscompose.ui.screens.WelcomeScreen
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WelcomeScreenUiTest {

    @Test
    fun welcomeScreenShowsBrandingAndButtons() = runComposeUiTest {
        setContent {
            MaterialTheme {
                WelcomeScreen()
            }
        }
        onNodeWithText("KeePass Compose").assertIsDisplayed()
        onNodeWithText("Open Database").assertIsDisplayed()
        onNodeWithText("New Database").assertIsDisplayed()
    }

    @Test
    fun openDatabaseButtonTriggersCallback() = runComposeUiTest {
        var clicked = false
        setContent {
            MaterialTheme {
                WelcomeScreen(onOpenDatabase = { clicked = true })
            }
        }
        onNodeWithText("Open Database").performClick()
        assertTrue(clicked)
    }

    @Test
    fun recentDatabasesAreDisplayed() = runComposeUiTest {
        val recents = listOf(
            RecentDatabase("MyDB", "/path/to/db.kdbx", "2025-01-01"),
            RecentDatabase("WorkDB", "/path/to/work.kdbx", "2025-02-15"),
        )
        setContent {
            MaterialTheme {
                WelcomeScreen(recentDatabases = recents)
            }
        }
        onNodeWithText("Recent Databases").assertIsDisplayed()
        onNodeWithText("MyDB").assertIsDisplayed()
        onNodeWithText("WorkDB").assertIsDisplayed()
    }
}
