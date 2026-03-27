package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.CryptoProvider
import org.github.keepasscompose.core.model.KdbxDatabase
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxIcon

/**
 * Imports KeePass 1.x (KDB) database files into the KdbxDatabase model.
 *
 * KDB is the legacy format used by KeePass 1.x. This importer reads the
 * binary KDB header, derives the master key from the password (and optional
 * key file), decrypts the payload using AES-256-CBC, and parses the
 * sequential group/entry records into a [KdbxDatabase].
 *
 * @param cryptoProvider Platform-specific cryptographic provider.
 */
class KdbImporter(private val cryptoProvider: CryptoProvider) {

    /**
     * Import a KDB file from raw bytes.
     *
     * @param data The raw KDB file bytes.
     * @param password The master password for the database.
     * @param keyFileData Optional key file data for composite key derivation.
     * @return A [KdbxDatabase] containing the imported groups and entries.
     * @throws KdbxParseException if the file is malformed or credentials are incorrect.
     */
    fun import(data: ByteArray, password: String, keyFileData: ByteArray? = null): KdbxDatabase {
        if (data.size < HEADER_SIZE) {
            throw KdbxParseException("File too small to be a valid KDB database")
        }

        val header = parseHeader(data)
        val encryptedPayload = data.copyOfRange(HEADER_SIZE, data.size)

        // Derive master key
        val masterKey = deriveMasterKey(header, password, keyFileData)

        // Decrypt
        val decrypted = cryptoProvider.aesDecrypt(encryptedPayload, masterKey, header.encryptionIv)

        // Verify content hash
        val contentHash = cryptoProvider.sha256(decrypted)
        if (!contentHash.contentEquals(header.contentHash)) {
            throw KdbxParseException(
                "Invalid credentials or corrupted database: content hash verification failed",
            )
        }

        // Parse groups and entries
        val groups = parseGroups(decrypted, header.groupCount)
        val entries = parseEntries(decrypted, header.groupCount, header.entryCount)

        // Build tree
        val rootGroup = buildGroupTree(groups, entries)

        return KdbxDatabase(rootGroup = rootGroup)
    }

    // -- Header parsing --

    /**
     * Parse the KDB file header.
     *
     * Header layout (124 bytes):
     * - [0..3]   signature1 (0x9AA2D903)
     * - [4..7]   signature2 (0xB54BFB65)
     * - [8..11]  flags
     * - [12..15] version
     * - [16..31] master seed (16 bytes)
     * - [32..47] encryption IV (16 bytes)
     * - [48..51] group count
     * - [52..55] entry count
     * - [56..87] content hash (32 bytes)
     * - [88..119] transform seed (32 bytes)
     * - [120..123] transform rounds
     */
    internal fun parseHeader(data: ByteArray): KdbHeader {
        val sig1 = readUInt32(data, 0)
        val sig2 = readUInt32(data, 4)

        if (sig1 != SIGNATURE_1) {
            throw KdbxParseException(
                "Invalid KDB signature1: expected 0x${SIGNATURE_1.toString(16)}, got 0x${sig1.toString(16)}",
            )
        }
        if (sig2 != SIGNATURE_2) {
            throw KdbxParseException(
                "Invalid KDB signature2: expected 0x${SIGNATURE_2.toString(16)}, got 0x${sig2.toString(16)}",
            )
        }

        val flags = readUInt32(data, 8)
        if (flags and FLAG_RIJNDAEL == 0L) {
            throw KdbxParseException("Only AES (Rijndael) encryption is supported for KDB files")
        }

        return KdbHeader(
            masterSeed = data.copyOfRange(16, 32),
            encryptionIv = data.copyOfRange(32, 48),
            groupCount = readUInt32(data, 48).toInt(),
            entryCount = readUInt32(data, 52).toInt(),
            contentHash = data.copyOfRange(56, 88),
            transformSeed = data.copyOfRange(88, 120),
            transformRounds = readUInt32(data, 120),
        )
    }

    // -- Key derivation --

    /**
     * Derive the final master key for AES decryption.
     *
     * KDB key derivation:
     * 1. compositeHash = SHA-256(SHA-256(password) [|| SHA-256(keyFile)])
     * 2. transformedKey = AES-KDF(compositeHash, transformSeed, transformRounds)
     * 3. transformedKeyHash = SHA-256(transformedKey)
     * 4. masterKey = SHA-256(masterSeed || transformedKeyHash)
     */
    private fun deriveMasterKey(header: KdbHeader, password: String, keyFileData: ByteArray?): ByteArray {
        val passwordHash = cryptoProvider.sha256(password.encodeToByteArray())

        val compositeBytes = if (keyFileData != null) {
            val keyFileHash = cryptoProvider.sha256(keyFileData)
            passwordHash + keyFileHash
        } else {
            passwordHash
        }
        val compositeHash = cryptoProvider.sha256(compositeBytes)

        val transformedKey = cryptoProvider.aesKdf(compositeHash, header.transformSeed, header.transformRounds)
        val transformedKeyHash = cryptoProvider.sha256(transformedKey)

        return cryptoProvider.sha256(header.masterSeed + transformedKeyHash)
    }

    // -- Group parsing --

    /**
     * Parse groups from the decrypted payload.
     *
     * Groups are serialized sequentially as TLV fields:
     * - 2-byte field type (little-endian)
     * - 4-byte field size (little-endian)
     * - field data
     *
     * Field types:
     * - 0x0001: GroupID (4 bytes, uint32)
     * - 0x0002: Name (null-terminated string)
     * - 0x0007: IconID (4 bytes, uint32)
     * - 0x0008: Level (2 bytes, uint16)
     * - 0xFFFF: End of group
     */
    private fun parseGroups(payload: ByteArray, groupCount: Int): List<KdbRawGroup> {
        val groups = mutableListOf<KdbRawGroup>()
        var offset = 0
        var currentGroupId = 0
        var currentName = ""
        var currentIconId = 0
        var currentLevel = 0

        var groupsParsed = 0
        while (groupsParsed < groupCount && offset + 6 <= payload.size) {
            val fieldType = readUInt16(payload, offset)
            val fieldSize = readUInt32(payload, offset + 2).toInt()
            offset += 6

            if (offset + fieldSize > payload.size) break

            when (fieldType) {
                FIELD_GROUP_ID -> {
                    if (fieldSize >= 4) {
                        currentGroupId = readUInt32(payload, offset).toInt()
                    }
                }

                FIELD_GROUP_NAME -> {
                    currentName = readString(payload, offset, fieldSize)
                }

                FIELD_GROUP_ICON -> {
                    if (fieldSize >= 4) {
                        currentIconId = readUInt32(payload, offset).toInt()
                    }
                }

                FIELD_GROUP_LEVEL -> {
                    if (fieldSize >= 2) {
                        currentLevel = readUInt16(payload, offset)
                    }
                }

                FIELD_END -> {
                    groups.add(
                        KdbRawGroup(
                            id = currentGroupId,
                            name = currentName,
                            iconId = currentIconId,
                            level = currentLevel,
                        ),
                    )
                    currentGroupId = 0
                    currentName = ""
                    currentIconId = 0
                    currentLevel = 0
                    groupsParsed++
                }
            }

            offset += fieldSize
        }

        return groups
    }

    // -- Entry parsing --

    /**
     * Parse entries from the decrypted payload.
     *
     * Entries follow groups in the payload with the same TLV structure.
     *
     * Field types:
     * - 0x0001: UUID (16 bytes)
     * - 0x0002: GroupID (4 bytes, uint32)
     * - 0x0003: IconID (4 bytes, uint32)
     * - 0x0004: Title (null-terminated string)
     * - 0x0005: URL (null-terminated string)
     * - 0x0006: UserName (null-terminated string)
     * - 0x0007: Password (null-terminated string)
     * - 0x0008: Notes (null-terminated string)
     * - 0xFFFF: End of entry
     */
    private fun parseEntries(payload: ByteArray, groupCount: Int, entryCount: Int): List<KdbRawEntry> {
        // Skip past groups first
        var offset = skipGroups(payload, groupCount)

        val entries = mutableListOf<KdbRawEntry>()
        var currentUuid = ""
        var currentGroupId = 0
        var currentIconId = 0
        var currentTitle = ""
        var currentUrl = ""
        var currentUserName = ""
        var currentPassword = ""
        var currentNotes = ""

        var entriesParsed = 0
        while (entriesParsed < entryCount && offset + 6 <= payload.size) {
            val fieldType = readUInt16(payload, offset)
            val fieldSize = readUInt32(payload, offset + 2).toInt()
            offset += 6

            if (offset + fieldSize > payload.size) break

            when (fieldType) {
                FIELD_ENTRY_UUID -> {
                    if (fieldSize >= 16) {
                        currentUuid = bytesToHex(payload.copyOfRange(offset, offset + 16))
                    }
                }

                FIELD_ENTRY_GROUP_ID -> {
                    if (fieldSize >= 4) {
                        currentGroupId = readUInt32(payload, offset).toInt()
                    }
                }

                FIELD_ENTRY_ICON -> {
                    if (fieldSize >= 4) {
                        currentIconId = readUInt32(payload, offset).toInt()
                    }
                }

                FIELD_ENTRY_TITLE -> {
                    currentTitle = readString(payload, offset, fieldSize)
                }

                FIELD_ENTRY_URL -> {
                    currentUrl = readString(payload, offset, fieldSize)
                }

                FIELD_ENTRY_USERNAME -> {
                    currentUserName = readString(payload, offset, fieldSize)
                }

                FIELD_ENTRY_PASSWORD -> {
                    currentPassword = readString(payload, offset, fieldSize)
                }

                FIELD_ENTRY_NOTES -> {
                    currentNotes = readString(payload, offset, fieldSize)
                }

                FIELD_END -> {
                    // Skip meta-stream entries (used by KeePass 1.x for internal data)
                    if (currentTitle != "Meta-Info" || currentUserName != "SYSTEM") {
                        entries.add(
                            KdbRawEntry(
                                uuid = currentUuid,
                                groupId = currentGroupId,
                                iconId = currentIconId,
                                title = currentTitle,
                                url = currentUrl,
                                userName = currentUserName,
                                password = currentPassword,
                                notes = currentNotes,
                            ),
                        )
                    }
                    currentUuid = ""
                    currentGroupId = 0
                    currentIconId = 0
                    currentTitle = ""
                    currentUrl = ""
                    currentUserName = ""
                    currentPassword = ""
                    currentNotes = ""
                    entriesParsed++
                }
            }

            offset += fieldSize
        }

        return entries
    }

    /**
     * Skip past all group records in the payload, returning the offset
     * where entry records begin.
     */
    private fun skipGroups(payload: ByteArray, groupCount: Int): Int {
        var offset = 0
        var groupsParsed = 0

        while (groupsParsed < groupCount && offset + 6 <= payload.size) {
            val fieldType = readUInt16(payload, offset)
            val fieldSize = readUInt32(payload, offset + 2).toInt()
            offset += 6

            if (offset + fieldSize > payload.size) break

            if (fieldType == FIELD_END) {
                groupsParsed++
            }

            offset += fieldSize
        }

        return offset
    }

    // -- Tree building --

    /**
     * Build a group tree from the flat parsed groups and entries.
     *
     * KDB groups have a level field indicating their depth in the hierarchy.
     * Level 0 groups are top-level children of the root.
     */
    private fun buildGroupTree(groups: List<KdbRawGroup>, entries: List<KdbRawEntry>): KdbxGroup {
        // Map entries to groups by groupId
        val entriesByGroup = entries.groupBy { it.groupId }

        // Convert raw groups to KdbxGroup
        val kdbxGroups = groups.map { rawGroup ->
            val groupEntries = entriesByGroup[rawGroup.id]?.map { rawEntry ->
                KdbxEntry(
                    uuid = rawEntry.uuid.ifEmpty { "kdb-${rawEntry.hashCode()}" },
                    icon = KdbxIcon(standardIndex = rawEntry.iconId),
                    fields = listOf(
                        KdbxEntryField(KdbxEntry.FIELD_TITLE, rawEntry.title),
                        KdbxEntryField(KdbxEntry.FIELD_USER_NAME, rawEntry.userName),
                        KdbxEntryField(KdbxEntry.FIELD_PASSWORD, rawEntry.password, isProtected = true),
                        KdbxEntryField(KdbxEntry.FIELD_URL, rawEntry.url),
                        KdbxEntryField(KdbxEntry.FIELD_NOTES, rawEntry.notes),
                    ),
                )
            } ?: emptyList()

            KdbxGroup(
                uuid = "kdb-group-${rawGroup.id}",
                name = rawGroup.name,
                icon = KdbxIcon(standardIndex = rawGroup.iconId),
                entries = groupEntries,
            )
        }

        // Build hierarchy based on level
        if (kdbxGroups.isEmpty()) {
            return KdbxGroup(uuid = "kdb-root", name = "Root")
        }

        return buildHierarchy(groups, kdbxGroups)
    }

    /**
     * Build the hierarchical tree using group levels.
     *
     * We use a stack-based approach: each group's level determines its
     * position relative to the previous groups.
     */
    private fun buildHierarchy(rawGroups: List<KdbRawGroup>, kdbxGroups: List<KdbxGroup>): KdbxGroup {
        val root = KdbxGroup(uuid = "kdb-root", name = "Root")
        if (rawGroups.isEmpty()) return root

        // Stack of (level, group) pairs, with groups accumulating children
        data class GroupNode(val level: Int, val group: KdbxGroup, val children: MutableList<KdbxGroup> = mutableListOf())

        val stack = mutableListOf<GroupNode>()
        // Virtual root at level -1
        val rootNode = GroupNode(-1, root)
        stack.add(rootNode)

        for (i in rawGroups.indices) {
            val level = rawGroups[i].level
            val group = kdbxGroups[i]

            // Pop groups from stack until we find the parent (level - 1)
            while (stack.size > 1 && stack.last().level >= level) {
                val child = stack.removeLast()
                val parent = stack.last()
                parent.children.add(child.group.copy(groups = child.children))
            }

            stack.add(GroupNode(level, group))
        }

        // Flush remaining stack
        while (stack.size > 1) {
            val child = stack.removeLast()
            val parent = stack.last()
            parent.children.add(child.group.copy(groups = child.children))
        }

        return root.copy(groups = rootNode.children)
    }

    // -- Binary read helpers --

    private fun readUInt16(data: ByteArray, offset: Int): Int = (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun readUInt32(data: ByteArray, offset: Int): Long = (data[offset].toLong() and 0xFF) or
        ((data[offset + 1].toLong() and 0xFF) shl 8) or
        ((data[offset + 2].toLong() and 0xFF) shl 16) or
        ((data[offset + 3].toLong() and 0xFF) shl 24)

    private fun readString(data: ByteArray, offset: Int, size: Int): String {
        if (size <= 0) return ""
        // Remove null terminator if present
        val end = if (size > 0 && data[offset + size - 1] == 0.toByte()) size - 1 else size
        return data.copyOfRange(offset, offset + end).decodeToString()
    }

    private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    // -- Internal data classes --

    internal data class KdbHeader(
        val masterSeed: ByteArray,
        val encryptionIv: ByteArray,
        val groupCount: Int,
        val entryCount: Int,
        val contentHash: ByteArray,
        val transformSeed: ByteArray,
        val transformRounds: Long,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KdbHeader) return false
            return masterSeed.contentEquals(other.masterSeed) &&
                encryptionIv.contentEquals(other.encryptionIv) &&
                groupCount == other.groupCount &&
                entryCount == other.entryCount &&
                contentHash.contentEquals(other.contentHash) &&
                transformSeed.contentEquals(other.transformSeed) &&
                transformRounds == other.transformRounds
        }

        override fun hashCode(): Int {
            var result = masterSeed.contentHashCode()
            result = 31 * result + encryptionIv.contentHashCode()
            result = 31 * result + groupCount
            result = 31 * result + entryCount
            result = 31 * result + contentHash.contentHashCode()
            result = 31 * result + transformSeed.contentHashCode()
            result = 31 * result + transformRounds.hashCode()
            return result
        }
    }

    private data class KdbRawGroup(val id: Int, val name: String, val iconId: Int, val level: Int)

    private data class KdbRawEntry(
        val uuid: String,
        val groupId: Int,
        val iconId: Int,
        val title: String,
        val url: String,
        val userName: String,
        val password: String,
        val notes: String,
    )

    companion object {
        // KDB file signatures
        internal const val SIGNATURE_1 = 0x9AA2D903L
        internal const val SIGNATURE_2 = 0xB54BFB65L

        // Header size in bytes
        private const val HEADER_SIZE = 124

        // Encryption flags
        private const val FLAG_RIJNDAEL = 2L

        // Group field types
        private const val FIELD_GROUP_ID = 0x0001
        private const val FIELD_GROUP_NAME = 0x0002
        private const val FIELD_GROUP_ICON = 0x0007
        private const val FIELD_GROUP_LEVEL = 0x0008
        private const val FIELD_END = 0xFFFF

        // Entry field types
        private const val FIELD_ENTRY_UUID = 0x0001
        private const val FIELD_ENTRY_GROUP_ID = 0x0002
        private const val FIELD_ENTRY_ICON = 0x0003
        private const val FIELD_ENTRY_TITLE = 0x0004
        private const val FIELD_ENTRY_URL = 0x0005
        private const val FIELD_ENTRY_USERNAME = 0x0006
        private const val FIELD_ENTRY_PASSWORD = 0x0007
        private const val FIELD_ENTRY_NOTES = 0x0008
    }
}
