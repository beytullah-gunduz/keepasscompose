package org.github.keepasscompose.core.common

import java.awt.Toolkit
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable

object OneTimeClipboard {

    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

    private val owner = ClipboardOwner { _, _ ->
        // Ownership lost means another app consumed/replaced the content.
        // Clear the clipboard so the sensitive data cannot be pasted again.
        try {
            clipboard.setContents(StringSelection(""), null)
        } catch (_: IllegalStateException) {
            // Clipboard may be unavailable momentarily; safe to ignore.
        }
    }

    fun copy(text: String) {
        clipboard.setContents(StringSelection(text), owner)
    }
}
