package org.github.keepasscompose.core.database

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.github.keepasscompose.core.model.DeletedObject
import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxMeta

/**
 * Result of writing the KDBX XML payload.
 *
 * @param xml The serialized XML string.
 */
data class KdbxXmlWriteResult(
    val xml: String,
)

/**
 * Serializes domain models back to the KeePass XML payload format.
 *
 * The XML payload contains the database metadata, group/entry tree, and deleted objects.
 * Protected field values (e.g., passwords) are encrypted using the inner random stream cipher
 * and written as base64-encoded ciphertext.
 *
 * @param isV4 Whether to write KDBX 4.x format (affects timestamp encoding).
 * @param innerStreamEncryptor Optional encryptor for protected field values.
 *   If null, protected values are written as plaintext (useful for testing).
 */
@OptIn(ExperimentalEncodingApi::class)
@Suppress("DEPRECATION")
class KdbxXmlWriter(
    private val isV4: Boolean = true,
    private val innerStreamEncryptor: InnerStreamEncryptor? = null,
) {

    fun writeXml(
        meta: KdbxMeta,
        rootGroup: KdbxGroup,
        deletedObjects: List<DeletedObject> = emptyList(),
        binaries: Map<Int, ByteArray> = emptyMap(),
    ): KdbxXmlWriteResult {
        val sb = StringBuilder()
        val writer = KtXmlWriter(sb, xmlVersion = XmlVersion.XML10)

        writer.startDocument(version = "1.0", encoding = "utf-8", standalone = null)
        writer.startTag("KeePassFile")

        writeMeta(writer, meta, binaries)
        writeRoot(writer, rootGroup, deletedObjects)

        writer.endTag("KeePassFile")
        writer.endDocument()
        writer.close()

        return KdbxXmlWriteResult(xml = sb.toString())
    }

    // -- Meta writing --

    private fun writeMeta(writer: XmlWriter, meta: KdbxMeta, binaries: Map<Int, ByteArray>) {
        writer.startTag("Meta")

        writer.writeElement("DatabaseName", meta.databaseName)
        writer.writeElement("DatabaseDescription", meta.description)
        writer.writeElement("DefaultUserName", meta.defaultUserName)
        writer.writeElement("RecycleBinEnabled", meta.recycleBinEnabled.toKdbxString())
        if (meta.recycleBinUuid != null) {
            writer.writeElement("RecycleBinUUID", meta.recycleBinUuid)
        }

        writeMemoryProtection(writer, meta)

        if (binaries.isNotEmpty()) {
            writeBinaries(writer, binaries)
        }

        writer.endTag("Meta")
    }

    private fun writeMemoryProtection(writer: XmlWriter, meta: KdbxMeta) {
        writer.startTag("MemoryProtection")
        writer.writeElement("ProtectTitle", meta.protectTitle.toKdbxString())
        writer.writeElement("ProtectUserName", meta.protectUserName.toKdbxString())
        writer.writeElement("ProtectPassword", meta.protectPassword.toKdbxString())
        writer.writeElement("ProtectURL", meta.protectUrl.toKdbxString())
        writer.writeElement("ProtectNotes", meta.protectNotes.toKdbxString())
        writer.endTag("MemoryProtection")
    }

    private fun writeBinaries(writer: XmlWriter, binaries: Map<Int, ByteArray>) {
        writer.startTag("Binaries")

        for ((id, data) in binaries.entries.sortedBy { it.key }) {
            val compressed = compressGzip(data)
            val useCompression = compressed.size < data.size

            writer.startTag("Binary")
            writer.attribute("ID", id.toString())
            if (useCompression) {
                writer.attribute("Compressed", "True")
                writer.text(Base64.encode(compressed))
            } else {
                writer.text(Base64.encode(data))
            }
            writer.endTag("Binary")
        }

        writer.endTag("Binaries")
    }

    // -- Root writing --

    private fun writeRoot(
        writer: XmlWriter,
        rootGroup: KdbxGroup,
        deletedObjects: List<DeletedObject>,
    ) {
        writer.startTag("Root")

        writeGroup(writer, rootGroup)

        if (deletedObjects.isNotEmpty()) {
            writeDeletedObjects(writer, deletedObjects)
        }

        writer.endTag("Root")
    }

    // -- Group writing --

    private fun writeGroup(writer: XmlWriter, group: KdbxGroup) {
        writer.startTag("Group")

        writer.writeElement("UUID", group.uuid)
        writer.writeElement("Name", group.name)
        writer.writeElement("Notes", group.notes)
        writer.writeElement("IconID", group.icon.standardIndex.toString())
        if (group.icon.customUuid != null) {
            writer.writeElement("CustomIconUUID", group.icon.customUuid)
        }

        for (subGroup in group.groups) {
            writeGroup(writer, subGroup)
        }

        for (entry in group.entries) {
            writeEntry(writer, entry)
        }

        writer.endTag("Group")
    }

    // -- Entry writing --

    private fun writeEntry(writer: XmlWriter, entry: KdbxEntry) {
        writer.startTag("Entry")

        writer.writeElement("UUID", entry.uuid)
        writer.writeElement("IconID", entry.icon.standardIndex.toString())
        if (entry.icon.customUuid != null) {
            writer.writeElement("CustomIconUUID", entry.icon.customUuid)
        }

        if (entry.tags.isNotEmpty()) {
            writer.writeElement("Tags", entry.tags.joinToString(";"))
        }

        writeTimes(writer, entry)

        for (field in entry.fields) {
            writeStringField(writer, field)
        }

        for (attachment in entry.attachments) {
            writeEntryBinary(writer, attachment)
        }

        if (entry.history.isNotEmpty()) {
            writer.startTag("History")
            for (historyEntry in entry.history) {
                writeEntry(writer, historyEntry)
            }
            writer.endTag("History")
        }

        writer.endTag("Entry")
    }

    private fun writeTimes(writer: XmlWriter, entry: KdbxEntry) {
        val hasTimes = entry.creationTime != null ||
            entry.lastModificationTime != null ||
            entry.lastAccessTime != null ||
            entry.expiryTime != null ||
            entry.expires

        if (!hasTimes) return

        writer.startTag("Times")

        entry.creationTime?.let {
            writer.writeElement("CreationTime", formatTimestamp(it))
        }
        entry.lastModificationTime?.let {
            writer.writeElement("LastModificationTime", formatTimestamp(it))
        }
        entry.lastAccessTime?.let {
            writer.writeElement("LastAccessTime", formatTimestamp(it))
        }
        entry.expiryTime?.let {
            writer.writeElement("ExpiryTime", formatTimestamp(it))
        }
        writer.writeElement("Expires", entry.expires.toKdbxString())

        writer.endTag("Times")
    }

    private fun writeStringField(writer: XmlWriter, field: KdbxEntryField) {
        writer.startTag("String")
        writer.writeElement("Key", field.key)

        writer.startTag("Value")
        if (field.isProtected) {
            writer.attribute("Protected", "True")
            if (field.value.isNotEmpty()) {
                val plaintext = field.value.encodeToByteArray()
                if (innerStreamEncryptor != null) {
                    val ciphertext = innerStreamEncryptor.encrypt(plaintext)
                    writer.text(Base64.encode(ciphertext))
                } else {
                    writer.text(field.value)
                }
            }
        } else {
            writer.text(field.value)
        }
        writer.endTag("Value")

        writer.endTag("String")
    }

    private fun writeEntryBinary(writer: XmlWriter, attachment: KdbxAttachment) {
        writer.startTag("Binary")
        writer.writeElement("Key", attachment.name)

        writer.startTag("Value")
        writer.attribute("Ref", attachment.id.toString())
        writer.endTag("Value")

        writer.endTag("Binary")
    }

    // -- DeletedObjects writing --

    private fun writeDeletedObjects(writer: XmlWriter, deletedObjects: List<DeletedObject>) {
        writer.startTag("DeletedObjects")

        for (obj in deletedObjects) {
            writer.startTag("DeletedObject")
            writer.writeElement("UUID", obj.uuid)
            obj.deletionTime?.let {
                writer.writeElement("DeletionTime", formatTimestamp(it))
            }
            writer.endTag("DeletedObject")
        }

        writer.endTag("DeletedObjects")
    }

    // -- Timestamp formatting --

    private fun formatTimestamp(instant: Instant): String {
        return if (isV4) {
            formatV4Timestamp(instant)
        } else {
            formatV3Timestamp(instant)
        }
    }

    private fun formatV3Timestamp(instant: Instant): String = instant.toString()

    private fun formatV4Timestamp(instant: Instant): String {
        val seconds = instant.epochSeconds + DOTNET_EPOCH_OFFSET
        val buffer = Buffer()
        buffer.writeLongLe(seconds)
        return Base64.encode(buffer.readByteArray())
    }

    // -- Utilities --

    private fun compressGzip(data: ByteArray): ByteArray {
        val buffer = Buffer()
        val gzipSink = GzipSink(buffer).buffer()
        gzipSink.write(data)
        gzipSink.close()
        return buffer.readByteArray()
    }

    companion object {
        /** Seconds between .NET epoch (0001-01-01) and Unix epoch (1970-01-01). */
        private const val DOTNET_EPOCH_OFFSET = 62135596800L
    }
}

// -- XmlWriter extension helpers --

private fun XmlWriter.startTag(name: String) {
    startTag("", name, "")
}

private fun XmlWriter.endTag(name: String) {
    endTag("", name, "")
}

private fun XmlWriter.attribute(name: String, value: String) {
    attribute("", name, "", value)
}

private fun XmlWriter.writeElement(name: String, value: String) {
    startTag(name)
    text(value)
    endTag(name)
}

private fun Boolean.toKdbxString(): String = if (this) "True" else "False"
