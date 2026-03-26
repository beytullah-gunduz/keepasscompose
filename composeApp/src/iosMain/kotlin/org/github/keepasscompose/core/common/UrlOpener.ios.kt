package org.github.keepasscompose.core.common

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val normalizedUrl = if (url.contains("://")) url else "https://$url"
    val nsUrl = NSURL.URLWithString(normalizedUrl) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
