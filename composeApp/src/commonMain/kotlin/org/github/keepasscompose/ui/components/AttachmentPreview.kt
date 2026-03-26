package org.github.keepasscompose.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.github.keepasscompose.core.model.KdbxAttachment
import org.jetbrains.skia.Image as SkiaImage

private enum class PreviewType { IMAGE, TEXT, UNSUPPORTED }

@Composable
fun AttachmentPreview(
    attachment: KdbxAttachment,
    modifier: Modifier = Modifier,
) {
    val previewType = remember(attachment.name) { detectPreviewType(attachment.name) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = attachment.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = formatFileSize(attachment.data.size.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (previewType) {
            PreviewType.IMAGE -> ImagePreview(attachment.data)
            PreviewType.TEXT -> TextPreview(attachment.data)
            PreviewType.UNSUPPORTED -> UnsupportedPreview()
        }
    }
}

@Composable
private fun ImagePreview(data: ByteArray) {
    val imageBitmap = remember(data) {
        try {
            SkiaImage.makeFromEncoded(data).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Image preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        UnsupportedPreview(message = "Failed to decode image")
    }
}

@Composable
private fun TextPreview(data: ByteArray) {
    val text = remember(data) {
        try {
            data.decodeToString().take(50_000) // Limit preview to 50K chars
        } catch (_: Exception) {
            null
        }
    }

    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
    } else {
        UnsupportedPreview(message = "Failed to decode text")
    }
}

@Composable
private fun UnsupportedPreview(message: String = "No preview available for this file type") {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun detectPreviewType(name: String): PreviewType {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "png", "jpg", "jpeg", "gif", "bmp", "webp" -> PreviewType.IMAGE
        "txt", "md", "csv", "log", "json", "xml" -> PreviewType.TEXT
        else -> PreviewType.UNSUPPORTED
    }
}
