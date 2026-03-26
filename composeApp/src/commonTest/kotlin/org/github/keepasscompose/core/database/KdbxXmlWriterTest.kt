package org.github.keepasscompose.core.database

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.github.keepasscompose.core.model.DeletedObject
import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta

@OptIn(ExperimentalEncodingApi::class)
class KdbxXmlWriterTest {

    // -- Meta writing tests --

    @Test
    fun writeMeta_databaseNameAndDescription() {
        val meta = KdbxMeta(
            databaseName = "Test Database",
            description = "My test DB",
            defaultUserName = "admin",
        )
        val result = writeAndReadBack(meta = meta)

        assertEquals("Test Database", result.meta.databaseName)
        assertEquals("My test DB", result.meta.description)
        assertEquals("admin", result.meta.defaultUserName)
    }

    @Test
    fun writeMeta_recycleBinEnabled() {
        val meta = KdbxMeta(
            recycleBinEnabled = true,
            recycleBinUuid = "AAAAAAAAAAAAAAAAAAAAAA==",
        )
        val result = writeAndReadBack(meta = meta)

        assertTrue(result.meta.recycleBinEnabled)
        assertEquals("AAAAAAAAAAAAAAAAAAAAAA==", result.meta.recycleBinUuid)
    }

    @Test
    fun writeMeta_recycleBinDisabled() {
        val meta = KdbxMeta(recycleBinEnabled = false)
        val result = writeAndReadBack(meta = meta)

        assertFalse(result.meta.recycleBinEnabled)
    }

    @Test
    fun writeMeta_memoryProtection() {
        val meta = KdbxMeta(
            protectTitle = true,
            protectUserName = true,
            protectPassword = true,
            protectUrl = false,
            protectNotes = false,
        )
        val result = writeAndReadBack(meta = meta)

        assertTrue(result.meta.protectTitle)
        assertTrue(result.meta.protectUserName)
        assertTrue(result.meta.protectPassword)
        assertFalse(result.meta.protectUrl)
        assertFalse(result.meta.protectNotes)
    }

    @Test
    fun writeMeta_historyMaxItems() {
        val meta = KdbxMeta(historyMaxItems = 25, historyMaxSize = 10485760)
        val result = writeAndReadBack(meta = meta)

        assertEquals(25, result.meta.historyMaxItems)
        assertEquals(10485760L, result.meta.historyMaxSize)
    }

    @Test
    fun writeMeta_historyMaxItems_defaults() {
        val meta = KdbxMeta()
        val result = writeAndReadBack(meta = meta)

        assertEquals(12, result.meta.historyMaxItems)
        assertEquals(6291456L, result.meta.historyMaxSize)
    }

    @Test
    fun writeMeta_historyMaxItems_negativeUnlimited() {
        val meta = KdbxMeta(historyMaxItems = -1, historyMaxSize = -1)
        val result = writeAndReadBack(meta = meta)

        assertEquals(-1, result.meta.historyMaxItems)
        assertEquals(-1L, result.meta.historyMaxSize)
    }

    @Test
    fun writeMeta_memoryProtection_allFalse() {
        val meta = KdbxMeta(
            protectTitle = false,
            protectUserName = false,
            protectPassword = false,
            protectUrl = false,
            protectNotes = false,
        )
        val result = writeAndReadBack(meta = meta)

        assertFalse(result.meta.protectTitle)
        assertFalse(result.meta.protectUserName)
        assertFalse(result.meta.protectPassword)
        assertFalse(result.meta.protectUrl)
        assertFalse(result.meta.protectNotes)
    }

    // -- Group writing tests --

    @Test
    fun writeGroup_basic() {
        val group = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            notes = "Root group notes",
            icon = KdbxIcon(standardIndex = 48),
        )
        val result = writeAndReadBack(rootGroup = group)

        assertEquals("cm9vdA==", result.rootGroup.uuid)
        assertEquals("Root", result.rootGroup.name)
        assertEquals("Root group notes", result.rootGroup.notes)
        assertEquals(48, result.rootGroup.icon.standardIndex)
    }

    @Test
    fun writeGroup_nestedGroups() {
        val group = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            groups = listOf(
                KdbxGroup(
                    uuid = "Y2hpbGQ=",
                    name = "Internet",
                    icon = KdbxIcon(standardIndex = 1),
                    groups = listOf(
                        KdbxGroup(
                            uuid = "Z3JhbmQ=",
                            name = "Social",
                            icon = KdbxIcon(standardIndex = 2),
                        ),
                    ),
                ),
                KdbxGroup(
                    uuid = "ZW1haWw=",
                    name = "Email",
                    icon = KdbxIcon(standardIndex = 3),
                ),
            ),
        )
        val result = writeAndReadBack(rootGroup = group)

        assertEquals(2, result.rootGroup.groups.size)
        assertEquals("Internet", result.rootGroup.groups[0].name)
        assertEquals("Email", result.rootGroup.groups[1].name)

        assertEquals(1, result.rootGroup.groups[0].groups.size)
        assertEquals("Social", result.rootGroup.groups[0].groups[0].name)
    }

    @Test
    fun writeGroup_customIcon() {
        val group = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            icon = KdbxIcon(standardIndex = 5, customUuid = "Y3VzdG9t"),
        )
        val result = writeAndReadBack(rootGroup = group)

        assertEquals(5, result.rootGroup.icon.standardIndex)
        assertEquals("Y3VzdG9t", result.rootGroup.icon.customUuid)
    }

    // -- Entry writing tests --

    @Test
    fun writeEntry_standardFields() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            icon = KdbxIcon(standardIndex = 0),
            fields = listOf(
                KdbxEntryField(key = "Title", value = "My Website"),
                KdbxEntryField(key = "UserName", value = "user@example.com"),
                KdbxEntryField(key = "Password", value = "secret123"),
                KdbxEntryField(key = "URL", value = "https://example.com"),
                KdbxEntryField(key = "Notes", value = "Some notes here"),
            ),
        )
        val result = writeAndReadBack(rootGroup = groupWith(entry))
        val readEntry = result.rootGroup.entries.first()

        assertEquals("ZW50cnk=", readEntry.uuid)
        assertEquals("My Website", readEntry.title)
        assertEquals("user@example.com", readEntry.userName)
        assertEquals("secret123", readEntry.password)
        assertEquals("https://example.com", readEntry.url)
        assertEquals("Some notes here", readEntry.notes)
        assertEquals(5, readEntry.fields.size)
    }

    @Test
    fun writeEntry_protectedField_withEncryptor() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            fields = listOf(
                KdbxEntryField(key = "Password", value = "secret123", isProtected = true),
            ),
        )

        // Identity encryptor: returns plaintext as-is
        val encryptor = InnerStreamEncryptor { it }
        val decryptor = InnerStreamDecryptor { it }

        val writer = KdbxXmlWriter(isV4 = false, innerStreamEncryptor = encryptor)
        val writeResult = writer.writeXml(
            meta = KdbxMeta(),
            rootGroup = groupWith(entry),
        )

        val reader = KdbxXmlReader(isV4 = false, innerStreamDecryptor = decryptor)
        val readResult = reader.readXml(writeResult.xml)
        val field = readResult.rootGroup.entries.first().fields.first()

        assertTrue(field.isProtected)
        assertEquals("secret123", field.value)
    }

    @Test
    fun writeEntry_protectedField_withoutEncryptor() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            fields = listOf(
                KdbxEntryField(key = "Password", value = "secret123", isProtected = true),
            ),
        )

        // No encryptor: value written as plaintext
        val writer = KdbxXmlWriter(isV4 = false)
        val writeResult = writer.writeXml(
            meta = KdbxMeta(),
            rootGroup = groupWith(entry),
        )

        // Read without decryptor - protected value is the raw text
        val reader = KdbxXmlReader(isV4 = false)
        val readResult = reader.readXml(writeResult.xml)
        val field = readResult.rootGroup.entries.first().fields.first()

        assertTrue(field.isProtected)
        assertEquals("secret123", field.value)
    }

    @Test
    fun writeEntry_protectedFields_encryptedInOrder() {
        val entries = listOf(
            KdbxEntry(
                uuid = "ZTE=",
                fields = listOf(KdbxEntryField("Password", "pass1", isProtected = true)),
            ),
            KdbxEntry(
                uuid = "ZTI=",
                fields = listOf(KdbxEntryField("Password", "pass2", isProtected = true)),
            ),
            KdbxEntry(
                uuid = "ZTM=",
                fields = listOf(KdbxEntryField("Password", "pass3", isProtected = true)),
            ),
        )

        // Track encryption and decryption order
        val encryptionOrder = mutableListOf<String>()
        val encryptor = InnerStreamEncryptor { plaintext ->
            encryptionOrder.add(plaintext.decodeToString())
            plaintext // identity
        }
        val decryptionOrder = mutableListOf<String>()
        val decryptor = InnerStreamDecryptor { ciphertext ->
            decryptionOrder.add(ciphertext.decodeToString())
            ciphertext // identity
        }

        val group = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            entries = entries,
        )
        val writer = KdbxXmlWriter(isV4 = false, innerStreamEncryptor = encryptor)
        val writeResult = writer.writeXml(meta = KdbxMeta(), rootGroup = group)

        val reader = KdbxXmlReader(isV4 = false, innerStreamDecryptor = decryptor)
        val readResult = reader.readXml(writeResult.xml)

        assertEquals(listOf("pass1", "pass2", "pass3"), encryptionOrder)
        assertEquals(listOf("pass1", "pass2", "pass3"), decryptionOrder)

        assertEquals("pass1", readResult.rootGroup.entries[0].password)
        assertEquals("pass2", readResult.rootGroup.entries[1].password)
        assertEquals("pass3", readResult.rootGroup.entries[2].password)
    }

    @Test
    fun writeEntry_tags() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            tags = listOf("banking", "finance", "important"),
        )
        val result = writeAndReadBack(rootGroup = groupWith(entry))

        assertEquals(listOf("banking", "finance", "important"), result.rootGroup.entries.first().tags)
    }

    @Test
    fun writeEntry_emptyTags_notWritten() {
        val entry = KdbxEntry(uuid = "ZW50cnk=", tags = emptyList())
        val writer = KdbxXmlWriter(isV4 = false)
        val writeResult = writer.writeXml(meta = KdbxMeta(), rootGroup = groupWith(entry))

        // Tags element should not be present when empty
        assertFalse(writeResult.xml.contains("<Tags>"))
    }

    @Test
    fun writeEntry_times_v3() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            creationTime = Instant.parse("2024-06-15T10:30:00Z"),
            lastModificationTime = Instant.parse("2024-06-16T12:00:00Z"),
            lastAccessTime = Instant.parse("2024-06-17T08:00:00Z"),
            expiryTime = Instant.parse("2025-06-15T10:30:00Z"),
            expires = true,
        )
        val result = writeAndReadBack(rootGroup = groupWith(entry), isV4 = false)
        val readEntry = result.rootGroup.entries.first()

        assertEquals(Instant.parse("2024-06-15T10:30:00Z"), readEntry.creationTime)
        assertEquals(Instant.parse("2024-06-16T12:00:00Z"), readEntry.lastModificationTime)
        assertEquals(Instant.parse("2024-06-17T08:00:00Z"), readEntry.lastAccessTime)
        assertEquals(Instant.parse("2025-06-15T10:30:00Z"), readEntry.expiryTime)
        assertTrue(readEntry.expires)
    }

    @Test
    fun writeEntry_times_v4() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            creationTime = Instant.parse("2024-01-01T00:00:00Z"),
            expires = false,
        )
        val result = writeAndReadBack(rootGroup = groupWith(entry), isV4 = true)
        val readEntry = result.rootGroup.entries.first()

        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), readEntry.creationTime)
        assertFalse(readEntry.expires)
    }

    @Test
    fun writeEntry_noTimes_omitted() {
        val entry = KdbxEntry(uuid = "ZW50cnk=")
        val writer = KdbxXmlWriter(isV4 = false)
        val writeResult = writer.writeXml(meta = KdbxMeta(), rootGroup = groupWith(entry))

        assertFalse(writeResult.xml.contains("<Times>"))
    }

    @Test
    fun writeEntry_customIcon() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            icon = KdbxIcon(standardIndex = 5, customUuid = "Y3VzdG9t"),
        )
        val result = writeAndReadBack(rootGroup = groupWith(entry))
        val readEntry = result.rootGroup.entries.first()

        assertEquals(5, readEntry.icon.standardIndex)
        assertEquals("Y3VzdG9t", readEntry.icon.customUuid)
    }

    @Test
    fun writeEntry_history() {
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            fields = listOf(KdbxEntryField("Title", "Current Title")),
            history = listOf(
                KdbxEntry(
                    uuid = "ZW50cnk=",
                    fields = listOf(KdbxEntryField("Title", "Old Title")),
                ),
            ),
        )
        val result = writeAndReadBack(rootGroup = groupWith(entry))
        val readEntry = result.rootGroup.entries.first()

        assertEquals("Current Title", readEntry.title)
        assertEquals(1, readEntry.history.size)
        assertEquals("Old Title", readEntry.history[0].title)
    }

    // -- Binary/Attachment tests --

    @Test
    fun writeBinaries_andEntryReference() {
        val binaryData = "Hello, World!".encodeToByteArray()
        val binaries = mapOf(0 to binaryData)

        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            attachments = listOf(
                KdbxAttachment(id = 0, name = "readme.txt", data = binaryData),
            ),
        )

        val writer = KdbxXmlWriter(isV4 = false)
        val writeResult = writer.writeXml(
            meta = KdbxMeta(),
            rootGroup = groupWith(entry),
            binaries = binaries,
        )

        val reader = KdbxXmlReader(isV4 = false)
        val readResult = reader.readXml(writeResult.xml)

        // Check binaries pool round-trips
        assertEquals(1, readResult.binaryPool.size)
        assertTrue(binaryData.contentEquals(readResult.binaryPool[0]!!))

        // Check entry attachment
        val attachment = readResult.rootGroup.entries.first().attachments.first()
        assertEquals("readme.txt", attachment.name)
        assertEquals(0, attachment.id)
        assertTrue(binaryData.contentEquals(attachment.data))
    }

    @Test
    fun writeBinaries_multipleBinaries() {
        val data0 = "First file".encodeToByteArray()
        val data1 = "Second file".encodeToByteArray()
        val binaries = mapOf(0 to data0, 1 to data1)

        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            attachments = listOf(
                KdbxAttachment(id = 0, name = "first.txt", data = data0),
                KdbxAttachment(id = 1, name = "second.txt", data = data1),
            ),
        )

        val writer = KdbxXmlWriter(isV4 = false)
        val writeResult = writer.writeXml(
            meta = KdbxMeta(),
            rootGroup = groupWith(entry),
            binaries = binaries,
        )

        val reader = KdbxXmlReader(isV4 = false)
        val readResult = reader.readXml(writeResult.xml)

        assertEquals(2, readResult.binaryPool.size)
        assertTrue(data0.contentEquals(readResult.binaryPool[0]!!))
        assertTrue(data1.contentEquals(readResult.binaryPool[1]!!))
    }

    // -- DeletedObjects tests --

    @Test
    fun writeDeletedObjects() {
        val deletedObjects = listOf(
            DeletedObject(
                uuid = "ZGVsZXRlZA==",
                deletionTime = Instant.parse("2024-03-15T14:00:00Z"),
            ),
            DeletedObject(
                uuid = "ZGVsZXRlZDI=",
                deletionTime = Instant.parse("2024-03-16T09:30:00Z"),
            ),
        )
        val result = writeAndReadBack(deletedObjects = deletedObjects, isV4 = false)

        assertEquals(2, result.deletedObjects.size)
        assertEquals("ZGVsZXRlZA==", result.deletedObjects[0].uuid)
        assertEquals(Instant.parse("2024-03-15T14:00:00Z"), result.deletedObjects[0].deletionTime)
        assertEquals("ZGVsZXRlZDI=", result.deletedObjects[1].uuid)
        assertEquals(Instant.parse("2024-03-16T09:30:00Z"), result.deletedObjects[1].deletionTime)
    }

    @Test
    fun writeDeletedObjects_empty_notWritten() {
        val writer = KdbxXmlWriter(isV4 = false)
        val writeResult = writer.writeXml(
            meta = KdbxMeta(),
            rootGroup = defaultRootGroup(),
        )

        assertFalse(writeResult.xml.contains("<DeletedObjects>"))
    }

    // -- XML structure test --

    @Test
    fun writeXml_containsKeePassFileRoot() {
        val writer = KdbxXmlWriter(isV4 = false)
        val result = writer.writeXml(
            meta = KdbxMeta(),
            rootGroup = defaultRootGroup(),
        )

        assertTrue(result.xml.contains("<KeePassFile>") || result.xml.contains("<KeePassFile"))
        assertTrue(result.xml.contains("</KeePassFile>"))
        assertTrue(result.xml.contains("<Meta>") || result.xml.contains("<Meta"))
        assertTrue(result.xml.contains("<Root>") || result.xml.contains("<Root"))
    }

    // -- Full integration test --

    @Test
    fun fullRoundTrip_integration() {
        val meta = KdbxMeta(
            databaseName = "My Passwords",
            description = "Personal password database",
            defaultUserName = "defaultuser",
            recycleBinEnabled = true,
            recycleBinUuid = "cmVjeWNsZQ==",
            protectTitle = false,
            protectUserName = false,
            protectPassword = true,
            protectUrl = false,
            protectNotes = false,
        )

        val rootGroup = KdbxGroup(
            uuid = "cm9vdA==",
            name = "Root",
            icon = KdbxIcon(standardIndex = 48),
            groups = listOf(
                KdbxGroup(
                    uuid = "aW50ZXJuZXQ=",
                    name = "Internet",
                    icon = KdbxIcon(standardIndex = 1),
                    entries = listOf(
                        KdbxEntry(
                            uuid = "Z21haWw=",
                            icon = KdbxIcon(standardIndex = 0),
                            tags = listOf("email", "google"),
                            creationTime = Instant.parse("2024-01-15T10:00:00Z"),
                            lastModificationTime = Instant.parse("2024-06-01T12:00:00Z"),
                            expires = false,
                            fields = listOf(
                                KdbxEntryField("Title", "Gmail"),
                                KdbxEntryField("UserName", "user@gmail.com"),
                                KdbxEntryField("Password", "mypassword"),
                                KdbxEntryField("URL", "https://mail.google.com"),
                            ),
                        ),
                    ),
                ),
                KdbxGroup(
                    uuid = "YmFua2luZw==",
                    name = "Banking",
                    icon = KdbxIcon(standardIndex = 37),
                ),
            ),
        )

        val deletedObjects = listOf(
            DeletedObject(
                uuid = "b2xkZW50cnk=",
                deletionTime = Instant.parse("2024-05-20T08:00:00Z"),
            ),
        )

        val result = writeAndReadBack(
            meta = meta,
            rootGroup = rootGroup,
            deletedObjects = deletedObjects,
            isV4 = false,
        )

        // Meta
        assertEquals("My Passwords", result.meta.databaseName)
        assertEquals("Personal password database", result.meta.description)
        assertEquals("defaultuser", result.meta.defaultUserName)
        assertTrue(result.meta.recycleBinEnabled)
        assertEquals("cmVjeWNsZQ==", result.meta.recycleBinUuid)
        assertTrue(result.meta.protectPassword)
        assertFalse(result.meta.protectTitle)

        // Root group
        assertEquals("Root", result.rootGroup.name)
        assertEquals(48, result.rootGroup.icon.standardIndex)
        assertEquals(2, result.rootGroup.groups.size)

        // Internet subgroup
        val internet = result.rootGroup.groups[0]
        assertEquals("Internet", internet.name)
        assertEquals(1, internet.entries.size)

        // Gmail entry
        val gmail = internet.entries[0]
        assertEquals("Gmail", gmail.title)
        assertEquals("user@gmail.com", gmail.userName)
        assertEquals("mypassword", gmail.password)
        assertEquals("https://mail.google.com", gmail.url)
        assertEquals(listOf("email", "google"), gmail.tags)
        assertNotNull(gmail.creationTime)
        assertFalse(gmail.expires)

        // Banking subgroup (empty)
        val banking = result.rootGroup.groups[1]
        assertEquals("Banking", banking.name)
        assertTrue(banking.entries.isEmpty())

        // Deleted objects
        assertEquals(1, result.deletedObjects.size)
        assertEquals("b2xkZW50cnk=", result.deletedObjects[0].uuid)
        assertEquals(Instant.parse("2024-05-20T08:00:00Z"), result.deletedObjects[0].deletionTime)
    }

    @Test
    fun fullRoundTrip_v4_integration() {
        val meta = KdbxMeta(databaseName = "V4 Database")
        val entry = KdbxEntry(
            uuid = "ZW50cnk=",
            creationTime = Instant.parse("2024-01-01T00:00:00Z"),
            lastModificationTime = Instant.parse("2024-06-15T12:00:00Z"),
            expires = false,
            fields = listOf(
                KdbxEntryField("Title", "Test Entry"),
                KdbxEntryField("Password", "v4pass", isProtected = true),
            ),
        )

        val encryptor = InnerStreamEncryptor { it }
        val decryptor = InnerStreamDecryptor { it }

        val writer = KdbxXmlWriter(isV4 = true, innerStreamEncryptor = encryptor)
        val writeResult = writer.writeXml(meta = meta, rootGroup = groupWith(entry))

        val reader = KdbxXmlReader(isV4 = true, innerStreamDecryptor = decryptor)
        val readResult = reader.readXml(writeResult.xml)

        assertEquals("V4 Database", readResult.meta.databaseName)
        val readEntry = readResult.rootGroup.entries.first()
        assertEquals("Test Entry", readEntry.title)
        assertEquals("v4pass", readEntry.password)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), readEntry.creationTime)
        assertEquals(Instant.parse("2024-06-15T12:00:00Z"), readEntry.lastModificationTime)
        assertFalse(readEntry.expires)
    }

    // -- Helpers --

    private fun defaultRootGroup() = KdbxGroup(uuid = "cm9vdA==", name = "Root")

    private fun groupWith(vararg entries: KdbxEntry) = KdbxGroup(
        uuid = "cm9vdA==",
        name = "Root",
        entries = entries.toList(),
    )

    private fun writeAndReadBack(
        meta: KdbxMeta = KdbxMeta(),
        rootGroup: KdbxGroup = defaultRootGroup(),
        deletedObjects: List<DeletedObject> = emptyList(),
        binaries: Map<Int, ByteArray> = emptyMap(),
        isV4: Boolean = false,
    ): KdbxXmlReadResult {
        val writer = KdbxXmlWriter(isV4 = isV4)
        val writeResult = writer.writeXml(meta, rootGroup, deletedObjects, binaries)

        val reader = KdbxXmlReader(isV4 = isV4)
        return reader.readXml(writeResult.xml)
    }
}
