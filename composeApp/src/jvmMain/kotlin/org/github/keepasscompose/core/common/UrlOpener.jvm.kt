package org.github.keepasscompose.core.common

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    val normalizedUrl = if (url.contains("://")) url else "https://$url"
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(normalizedUrl))
    }
}
