package org.github.keepasscompose.core.common

import platform.UIKit.UIPasteboard

actual class ClipboardManager actual constructor() {

    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun clearClipboard() {
        UIPasteboard.generalPasteboard.string = ""
    }
}
