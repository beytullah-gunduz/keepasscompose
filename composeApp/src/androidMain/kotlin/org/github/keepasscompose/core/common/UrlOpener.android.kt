package org.github.keepasscompose.core.common

import android.content.Intent
import android.net.Uri

actual fun openUrl(url: String) {
    val normalizedUrl = if (url.contains("://")) url else "https://$url"
    val context = AndroidContextHolder.applicationContext ?: return
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(intent)
}
