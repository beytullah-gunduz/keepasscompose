package org.github.keepasscompose.core.common

import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionExpirationDate
import platform.UIKit.UIPasteboardOptionLocalOnly

actual class ClipboardManager actual constructor() {
    actual fun copyToClipboard(text: String) {
        // Set pasteboard item with local-only option and short expiration
        // to prevent clipboard content from syncing via Universal Clipboard
        // and to auto-expire after 2 minutes.
        UIPasteboard.generalPasteboard.setItems(
            listOf<Map<Any?, Any>>(mapOf("public.utf8-plain-text" to text)),
            mapOf<Any?, Any>(
                UIPasteboardOptionLocalOnly to true,
                UIPasteboardOptionExpirationDate to NSDate().dateByAddingTimeInterval(120.0),
            ),
        )
    }

    actual fun clearClipboard() {
        UIPasteboard.generalPasteboard.string = ""
    }
}
