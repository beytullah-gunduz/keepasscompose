package org.github.keepasscompose.core.common

import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval
import platform.UIKit.UIPasteboard

actual class ClipboardManager actual constructor() {
    actual fun copyToClipboard(text: String) {
        // Set pasteboard item with local-only option and short expiration
        // to prevent clipboard content from syncing via Universal Clipboard
        // and to auto-expire after 2 minutes.
        UIPasteboard.generalPasteboard.setItems(
            listOf(mapOf("public.utf8-plain-text" to text)),
            mapOf(
                UIPasteboard.localOnly to true,
                UIPasteboard.expirationDate to NSDate().dateByAddingTimeInterval(120.0),
            ),
        )
    }

    actual fun clearClipboard() {
        UIPasteboard.generalPasteboard.string = ""
    }
}
