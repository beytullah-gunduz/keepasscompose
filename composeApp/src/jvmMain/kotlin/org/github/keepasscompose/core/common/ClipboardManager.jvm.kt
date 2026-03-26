package org.github.keepasscompose.core.common

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual class ClipboardManager actual constructor() {

    private val clipboard: java.awt.datatransfer.Clipboard
        get() = Toolkit.getDefaultToolkit().systemClipboard

    actual fun copyToClipboard(text: String) {
        clipboard.setContents(StringSelection(text), null)
    }

    actual fun clearClipboard() {
        clipboard.setContents(StringSelection(""), null)
    }
}
