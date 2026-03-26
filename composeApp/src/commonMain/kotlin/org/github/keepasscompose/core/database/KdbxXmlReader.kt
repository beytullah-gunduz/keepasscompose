package org.github.keepasscompose.core.database

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlStreaming
import okio.Buffer
import org.github.keepasscompose.core.model.DeletedObject
import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

/**
 * Result of parsing the KDBX XML payload.
 *
 * @param meta The parsed database metadata.
 * @param rootGroup The root group containing all groups and entries.
 * @param deletedObjects Objects that were deleted from the database.
 * @param binaryPool The binary attachment pool, populated from either
 *   XML Meta/Binaries (KDBX 3.1) or the inner header (KDBX 4.x).
 */
data class KdbxXmlReadResult(
    val meta: KdbxMeta,
    val rootGroup: KdbxGroup,
    val deletedObjects: List<DeletedObject> = emptyList(),
    val binaryPool: KdbxBinaryPool = KdbxBinaryPool(),
)

/**
 * Parses the decrypted and decompressed KDBX XML payload into domain models.
 *
 * The XML payload contains the database metadata, group/entry tree, and deleted objects.
 * Protected field values (e.g., passwords) are base64-encoded ciphertext that must be
 * decrypted using the inner random stream cipher.
 *
 * @param isV4 Whether the source file is KDBX 4.x (affects timestamp format).
 * @param innerStreamDecryptor Optional decryptor for protected field values.
 *   If null, protected values are left as raw base64 strings.
 * @param externalBinaryPool Optional pre-populated binary pool from the KDBX 4.x inner header.
 *   When provided, entry attachment references resolve against this pool instead of
 *   parsing binaries from XML Meta/Binaries.
 */
@OptIn(ExperimentalEncodingApi::class)
class KdbxXmlReader(
    private val isV4: Boolean = true,
    private val innerStreamDecryptor: InnerStreamDecryptor? = null,
    private val externalBinaryPool: KdbxBinaryPool? = null,
) {
    private var binariesPool: KdbxBinaryPool = externalBinaryPool ?: KdbxBinaryPool()

    fun readXml(xml: String): KdbxXmlReadResult {
        val reader = xmlStreaming.newReader(xml)

        reader.nextTag()
        reader.requireStart("KeePassFile")

        var meta = KdbxMeta()
        var rootGroup: KdbxGroup? = null
        var deletedObjects = emptyList<DeletedObject>()

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "Meta" -> {
                    meta = parseMeta(reader)
                }

                "Root" -> {
                    val result = parseRoot(reader)
                    rootGroup = result.first
                    deletedObjects = result.second
                }

                else -> {
                    reader.skipElement()
                }
            }
        }

        reader.close()

        return KdbxXmlReadResult(
            meta = meta,
            rootGroup = rootGroup ?: throw KdbxParseException("Missing Root/Group element"),
            deletedObjects = deletedObjects,
            binaryPool = binariesPool,
        )
    }

    // -- Meta parsing --

    private fun parseMeta(reader: XmlReader): KdbxMeta {
        reader.requireStart("Meta")

        var databaseName = ""
        var description = ""
        var defaultUserName = ""
        var historyMaxItems = KdbxMeta.DEFAULT_HISTORY_MAX_ITEMS
        var historyMaxSize = KdbxMeta.DEFAULT_HISTORY_MAX_SIZE
        var recycleBinEnabled = true
        var recycleBinUuid: String? = null
        var protectTitle = false
        var protectUserName = false
        var protectPassword = true
        var protectUrl = false
        var protectNotes = false

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "DatabaseName" -> {
                    databaseName = reader.readElementText()
                }

                "DatabaseDescription" -> {
                    description = reader.readElementText()
                }

                "DefaultUserName" -> {
                    defaultUserName = reader.readElementText()
                }

                "HistoryMaxItems" -> {
                    historyMaxItems = reader.readElementText().toIntOrNull()
                        ?: KdbxMeta.DEFAULT_HISTORY_MAX_ITEMS
                }

                "HistoryMaxSize" -> {
                    historyMaxSize = reader.readElementText().toLongOrNull()
                        ?: KdbxMeta.DEFAULT_HISTORY_MAX_SIZE
                }

                "RecycleBinEnabled" -> {
                    recycleBinEnabled = reader.readElementText().toBooleanKdbx()
                }

                "RecycleBinUUID" -> {
                    recycleBinUuid = reader.readElementText().ifBlank { null }
                }

                "MemoryProtection" -> {
                    val mp = parseMemoryProtection(reader)
                    protectTitle = mp.protectTitle
                    protectUserName = mp.protectUserName
                    protectPassword = mp.protectPassword
                    protectUrl = mp.protectUrl
                    protectNotes = mp.protectNotes
                }

                "Binaries" -> {
                    parseBinariesIntoPool(reader)
                }

                else -> {
                    reader.skipElement()
                }
            }
        }

        return KdbxMeta(
            databaseName = databaseName,
            description = description,
            defaultUserName = defaultUserName,
            historyMaxItems = historyMaxItems,
            historyMaxSize = historyMaxSize,
            recycleBinEnabled = recycleBinEnabled,
            recycleBinUuid = recycleBinUuid,
            protectTitle = protectTitle,
            protectUserName = protectUserName,
            protectPassword = protectPassword,
            protectUrl = protectUrl,
            protectNotes = protectNotes,
        )
    }

    private data class MemoryProtection(
        val protectTitle: Boolean = false,
        val protectUserName: Boolean = false,
        val protectPassword: Boolean = true,
        val protectUrl: Boolean = false,
        val protectNotes: Boolean = false,
    )

    private fun parseMemoryProtection(reader: XmlReader): MemoryProtection {
        reader.requireStart("MemoryProtection")

        var protectTitle = false
        var protectUserName = false
        var protectPassword = true
        var protectUrl = false
        var protectNotes = false

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "ProtectTitle" -> protectTitle = reader.readElementText().toBooleanKdbx()
                "ProtectUserName" -> protectUserName = reader.readElementText().toBooleanKdbx()
                "ProtectPassword" -> protectPassword = reader.readElementText().toBooleanKdbx()
                "ProtectURL" -> protectUrl = reader.readElementText().toBooleanKdbx()
                "ProtectNotes" -> protectNotes = reader.readElementText().toBooleanKdbx()
                else -> reader.skipElement()
            }
        }

        return MemoryProtection(protectTitle, protectUserName, protectPassword, protectUrl, protectNotes)
    }

    private fun parseBinariesIntoPool(reader: XmlReader) {
        reader.requireStart("Binaries")

        while (reader.nextTag() == EventType.START_ELEMENT) {
            if (reader.localName == "Binary") {
                val id = reader.getAttributeValue(null, "ID")?.toIntOrNull()
                val compressed = reader.getAttributeValue(null, "Compressed")?.toBooleanKdbx() ?: false
                val base64Text = reader.readElementText()

                if (id != null && base64Text.isNotBlank()) {
                    var data = Base64.decode(base64Text)
                    if (compressed) {
                        data = KdbxBinaryPool.decompressGzip(data)
                    }
                    binariesPool.put(id, data)
                }
            } else {
                reader.skipElement()
            }
        }
    }

    // -- Root parsing --

    private fun parseRoot(reader: XmlReader): Pair<KdbxGroup?, List<DeletedObject>> {
        reader.requireStart("Root")

        var rootGroup: KdbxGroup? = null
        var deletedObjects = emptyList<DeletedObject>()

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "Group" -> rootGroup = parseGroup(reader)
                "DeletedObjects" -> deletedObjects = parseDeletedObjects(reader)
                else -> reader.skipElement()
            }
        }

        return rootGroup to deletedObjects
    }

    // -- Group parsing --

    private fun parseGroup(reader: XmlReader): KdbxGroup {
        reader.requireStart("Group")

        var uuid = ""
        var name = ""
        var notes = ""
        var iconId = 0
        var customIconUuid: String? = null
        val groups = mutableListOf<KdbxGroup>()
        val entries = mutableListOf<KdbxEntry>()

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "UUID" -> uuid = reader.readElementText()
                "Name" -> name = reader.readElementText()
                "Notes" -> notes = reader.readElementText()
                "IconID" -> iconId = reader.readElementText().toIntOrNull() ?: 0
                "CustomIconUUID" -> customIconUuid = reader.readElementText().ifBlank { null }
                "Group" -> groups.add(parseGroup(reader))
                "Entry" -> entries.add(parseEntry(reader))
                else -> reader.skipElement()
            }
        }

        return KdbxGroup(
            uuid = uuid,
            name = name,
            icon = KdbxIcon(standardIndex = iconId, customUuid = customIconUuid),
            notes = notes,
            groups = groups,
            entries = entries,
        )
    }

    // -- Entry parsing --

    private fun parseEntry(reader: XmlReader): KdbxEntry {
        reader.requireStart("Entry")

        var uuid = ""
        var iconId = 0
        var customIconUuid: String? = null
        var tags = emptyList<String>()
        val fields = mutableListOf<KdbxEntryField>()
        val attachments = mutableListOf<KdbxAttachment>()
        val history = mutableListOf<KdbxEntry>()
        var creationTime: Instant? = null
        var lastModificationTime: Instant? = null
        var lastAccessTime: Instant? = null
        var expiryTime: Instant? = null
        var expires = false

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "UUID" -> {
                    uuid = reader.readElementText()
                }

                "IconID" -> {
                    iconId = reader.readElementText().toIntOrNull() ?: 0
                }

                "CustomIconUUID" -> {
                    customIconUuid = reader.readElementText().ifBlank { null }
                }

                "Tags" -> {
                    val tagText = reader.readElementText()
                    tags =
                        if (tagText.isBlank()) {
                            emptyList()
                        } else {
                            tagText.split(";").filter { it.isNotBlank() }
                        }
                }

                "Times" -> {
                    val times = parseTimes(reader)
                    creationTime = times.creationTime
                    lastModificationTime = times.lastModificationTime
                    lastAccessTime = times.lastAccessTime
                    expiryTime = times.expiryTime
                    expires = times.expires
                }

                "String" -> {
                    fields.add(parseStringField(reader))
                }

                "Binary" -> {
                    val attachment = parseEntryBinary(reader)
                    if (attachment != null) attachments.add(attachment)
                }

                "History" -> {
                    while (reader.nextTag() == EventType.START_ELEMENT) {
                        if (reader.localName == "Entry") {
                            history.add(parseEntry(reader))
                        } else {
                            reader.skipElement()
                        }
                    }
                }

                else -> {
                    reader.skipElement()
                }
            }
        }

        return KdbxEntry(
            uuid = uuid,
            icon = KdbxIcon(standardIndex = iconId, customUuid = customIconUuid),
            tags = tags,
            fields = fields,
            attachments = attachments,
            history = history,
            creationTime = creationTime,
            lastModificationTime = lastModificationTime,
            lastAccessTime = lastAccessTime,
            expiryTime = expiryTime,
            expires = expires,
        )
    }

    private data class Times(
        val creationTime: Instant? = null,
        val lastModificationTime: Instant? = null,
        val lastAccessTime: Instant? = null,
        val expiryTime: Instant? = null,
        val expires: Boolean = false,
    )

    private fun parseTimes(reader: XmlReader): Times {
        reader.requireStart("Times")

        var creationTime: Instant? = null
        var lastModificationTime: Instant? = null
        var lastAccessTime: Instant? = null
        var expiryTime: Instant? = null
        var expires = false

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "CreationTime" -> creationTime = parseTimestamp(reader.readElementText())
                "LastModificationTime" -> lastModificationTime = parseTimestamp(reader.readElementText())
                "LastAccessTime" -> lastAccessTime = parseTimestamp(reader.readElementText())
                "ExpiryTime" -> expiryTime = parseTimestamp(reader.readElementText())
                "Expires" -> expires = reader.readElementText().toBooleanKdbx()
                else -> reader.skipElement()
            }
        }

        return Times(creationTime, lastModificationTime, lastAccessTime, expiryTime, expires)
    }

    private fun parseStringField(reader: XmlReader): KdbxEntryField {
        reader.requireStart("String")

        var key = ""
        var value = ""
        var isProtected = false

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "Key" -> {
                    key = reader.readElementText()
                }

                "Value" -> {
                    isProtected = reader
                        .getAttributeValue(null, "Protected")
                        ?.toBooleanKdbx() ?: false
                    val rawText = reader.readElementText()

                    value =
                        if (isProtected && rawText.isNotBlank() && innerStreamDecryptor != null) {
                            val ciphertext = Base64.decode(rawText)
                            val plaintext = innerStreamDecryptor.decrypt(ciphertext)
                            plaintext.decodeToString()
                        } else {
                            rawText
                        }
                }

                else -> {
                    reader.skipElement()
                }
            }
        }

        return KdbxEntryField(key = key, value = value, isProtected = isProtected)
    }

    private fun parseEntryBinary(reader: XmlReader): KdbxAttachment? {
        reader.requireStart("Binary")

        var name = ""
        var refId: Int? = null

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "Key" -> {
                    name = reader.readElementText()
                }

                "Value" -> {
                    refId = reader.getAttributeValue(null, "Ref")?.toIntOrNull()
                    reader.readElementText() // consume content (may be empty for Ref)
                }

                else -> {
                    reader.skipElement()
                }
            }
        }

        if (refId == null) return null

        return KdbxAttachment(
            id = refId,
            name = name,
            data = binariesPool[refId] ?: byteArrayOf(),
        )
    }

    // -- DeletedObjects parsing --

    private fun parseDeletedObjects(reader: XmlReader): List<DeletedObject> {
        reader.requireStart("DeletedObjects")
        val objects = mutableListOf<DeletedObject>()

        while (reader.nextTag() == EventType.START_ELEMENT) {
            if (reader.localName == "DeletedObject") {
                objects.add(parseDeletedObject(reader))
            } else {
                reader.skipElement()
            }
        }

        return objects
    }

    private fun parseDeletedObject(reader: XmlReader): DeletedObject {
        reader.requireStart("DeletedObject")

        var uuid = ""
        var deletionTime: Instant? = null

        while (reader.nextTag() == EventType.START_ELEMENT) {
            when (reader.localName) {
                "UUID" -> uuid = reader.readElementText()
                "DeletionTime" -> deletionTime = parseTimestamp(reader.readElementText())
                else -> reader.skipElement()
            }
        }

        return DeletedObject(uuid = uuid, deletionTime = deletionTime)
    }

    // -- Timestamp parsing --

    private fun parseTimestamp(text: String): Instant? {
        if (text.isBlank()) return null

        return if (isV4) {
            parseV4Timestamp(text)
        } else {
            parseV3Timestamp(text)
        }
    }

    private fun parseV3Timestamp(text: String): Instant? = try {
        Instant.parse(text)
    } catch (_: Exception) {
        null
    }

    private fun parseV4Timestamp(text: String): Instant? = try {
        val bytes = Base64.decode(text)
        if (bytes.size != 8) return null
        val seconds = Buffer().apply { write(bytes) }.readLongLe()
        Instant.fromEpochSeconds(seconds - DOTNET_EPOCH_OFFSET)
    } catch (_: Exception) {
        null
    }

    companion object {
        /** Seconds between .NET epoch (0001-01-01) and Unix epoch (1970-01-01). */
        private const val DOTNET_EPOCH_OFFSET = 62135596800L
    }
}

// -- XmlReader extension helpers --

private fun XmlReader.requireStart(name: String) {
    if (eventType != EventType.START_ELEMENT || localName != name) {
        throw KdbxParseException("Expected <$name>, got <$localName> ($eventType)")
    }
}

private fun XmlReader.readElementText(): String {
    val sb = StringBuilder()
    while (true) {
        when (next()) {
            EventType.TEXT, EventType.CDSECT, EventType.ENTITY_REF -> {
                sb.append(text)
            }

            EventType.END_ELEMENT -> {
                return sb.toString()
            }

            EventType.START_ELEMENT -> {
                throw KdbxParseException("Unexpected child element: $localName")
            }

            else -> {} // ignore processing instructions, comments, etc.
        }
    }
}

private fun XmlReader.skipElement() {
    var depth = 1
    while (depth > 0) {
        when (next()) {
            EventType.START_ELEMENT -> {
                depth++
            }

            EventType.END_ELEMENT -> {
                depth--
            }

            else -> {}
        }
    }
}

private fun String.toBooleanKdbx(): Boolean = equals("True", ignoreCase = true) || equals("true")
