package org.github.keepasscompose.ui.shortcuts

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import org.github.keepasscompose.ui.components.ToolbarCallbacks

fun Modifier.keyboardShortcuts(callbacks: ToolbarCallbacks, hasSelectedEntry: Boolean = false): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

    when {
        event.isCtrlPressed -> {
            when (event.key) {
                Key.N -> {
                    callbacks.onNewDatabase()
                    true
                }

                Key.O -> {
                    callbacks.onOpenDatabase()
                    true
                }

                Key.S -> {
                    callbacks.onSaveDatabase()
                    true
                }

                Key.L -> {
                    callbacks.onLockDatabase()
                    true
                }

                Key.F -> {
                    callbacks.onSearch()
                    true
                }

                Key.B -> {
                    if (hasSelectedEntry) {
                        callbacks.onCopyUsername()
                        true
                    } else {
                        false
                    }
                }

                Key.C -> {
                    if (hasSelectedEntry) {
                        callbacks.onCopyPassword()
                        true
                    } else {
                        false
                    }
                }

                Key.E -> {
                    if (hasSelectedEntry) {
                        callbacks.onEditEntry()
                        true
                    } else {
                        false
                    }
                }

                else -> {
                    false
                }
            }
        }

        event.key == Key.Delete -> {
            if (hasSelectedEntry) {
                callbacks.onDeleteEntry()
                true
            } else {
                false
            }
        }

        else -> {
            false
        }
    }
}
