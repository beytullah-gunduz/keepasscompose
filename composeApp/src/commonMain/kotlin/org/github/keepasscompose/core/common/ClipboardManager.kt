package org.github.keepasscompose.core.common

expect class ClipboardManager() {
    fun copyToClipboard(text: String)
    fun clearClipboard()
}
