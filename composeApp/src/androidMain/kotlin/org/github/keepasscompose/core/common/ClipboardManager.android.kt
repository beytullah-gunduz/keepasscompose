package org.github.keepasscompose.core.common

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.content.ClipboardManager as AndroidClipboardManager

actual class ClipboardManager actual constructor() {
    private val clipboard: AndroidClipboardManager
        get() =
            AndroidContextHolder.applicationContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager

    actual fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("KeePassCompose", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clip.description.extras =
                android.os.PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
        }
        clipboard.setPrimaryClip(clip)
    }

    actual fun clearClipboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
