package org.github.keepasscompose.core.common

import android.content.Intent
import android.net.Uri

/**
 * Handles Android intents for opening KDBX database files.
 *
 * When the app is launched via a VIEW intent (e.g., from a file manager
 * or email attachment), this handler extracts the file URI and converts
 * it to a format the app can use to open the database.
 */
object IntentHandler {
    /**
     * Result of processing an intent.
     */
    sealed class IntentResult {
        /** The intent contains a KDBX file to open. */
        data class OpenDatabase(val uri: Uri) : IntentResult()

        /** The intent does not contain a supported action. */
        data object None : IntentResult()
    }

    /**
     * Process an incoming intent and determine the action to take.
     *
     * @param intent The Android intent to process.
     * @return The result indicating what action to take.
     */
    fun processIntent(intent: Intent?): IntentResult {
        if (intent == null) return IntentResult.None

        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null && isKdbxUri(uri, intent)) {
                    IntentResult.OpenDatabase(uri)
                } else {
                    IntentResult.None
                }
            }

            else -> IntentResult.None
        }
    }

    /**
     * Check if a URI points to a KDBX file.
     */
    private fun isKdbxUri(uri: Uri, intent: Intent): Boolean {
        // Check MIME type
        val mimeType = intent.type
        if (mimeType == "application/x-keepass") return true

        // Check file extension
        val path = uri.path ?: uri.toString()
        if (path.endsWith(".kdbx", ignoreCase = true)) return true
        if (path.endsWith(".kdb", ignoreCase = true)) return true

        // For octet-stream, we accept it as it may be a KDBX file
        if (mimeType == "application/octet-stream") return true

        return false
    }
}
