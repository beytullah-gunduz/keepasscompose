package org.github.keepasscompose.core.common

import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

actual class FilePicker actual constructor() {

    actual suspend fun pickFile(extensions: List<String>): String? = suspendCancellableCoroutine { cont ->
        val activity = AndroidContextHolder.activity
        if (activity == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val key = "file_pick_${UUID.randomUUID()}"
        val mimeTypes = extensions.map { extensionToMimeType(it) }.toTypedArray()
            .ifEmpty { arrayOf("*/*") }

        val launcher = activity.activityResultRegistry.register(key, ActivityResultContracts.OpenDocument()) { uri ->
            val path = uri?.toString()
            // Take persistable read permission so the URI works across app restarts
            if (uri != null) {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // Best-effort; some providers don't support persistable permissions
                }
            }
            cont.resume(path)
        }

        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(mimeTypes)
    }

    actual suspend fun pickSaveLocation(defaultName: String, extensions: List<String>): String? = suspendCancellableCoroutine { cont ->
        val activity = AndroidContextHolder.activity
        if (activity == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val key = "file_save_${UUID.randomUUID()}"
        val mimeType = extensions.firstOrNull()?.let { extensionToMimeType(it) } ?: "*/*"

        val launcher = activity.activityResultRegistry.register(key, ActivityResultContracts.CreateDocument(mimeType)) { uri ->
            val path = uri?.toString()
            if (uri != null) {
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // Best-effort
                }
            }
            cont.resume(path)
        }

        cont.invokeOnCancellation { launcher.unregister() }
        launcher.launch(defaultName)
    }

    private fun extensionToMimeType(ext: String): String = when (ext.lowercase()) {
        // kdbx/kdb have no standard MIME type registered on Android;
        // using application/octet-stream so the picker allows selecting them.
        "kdbx", "kdb", "keyx", "key" -> "application/octet-stream"

        "xml" -> "text/xml"

        "csv" -> "text/csv"

        "html", "htm" -> "text/html"

        "json" -> "application/json"

        else -> "application/octet-stream"
    }
}
