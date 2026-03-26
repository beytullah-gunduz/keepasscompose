package org.github.keepasscompose.core.database

import kotlin.time.Instant
import org.github.keepasscompose.core.model.KdbxEntry
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class KdbxXmlReaderTest {

    // -- Meta parsing tests --

    @Test
    fun parseMeta_databaseName() {
        val xml = wrapKeePassFile(
            meta = """
                <DatabaseName>Test Database</DatabaseName>
                <DatabaseDescription>My test DB</DatabaseDescription>
                <DefaultUserName>admin</DefaultUserName>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals("Test Database", result.meta.databaseName)
        assertEquals("My test DB", result.meta.description)
        assertEquals("admin", result.meta.defaultUserName)
    }

    @Test
    fun parseMeta_recycleBin() {
        val xml = wrapKeePassFile(
            meta = """
                <RecycleBinEnabled>True</RecycleBinEnabled>
                <RecycleBinUUID>AAAAAAAAAAAAAAAAAAAAAA==</RecycleBinUUID>
            """,
        )
        val result = readerV3().readXml(xml)

        assertTrue(result.meta.recycleBinEnabled)
        assertEquals("AAAAAAAAAAAAAAAAAAAAAA==", result.meta.recycleBinUuid)
    }

    @Test
    fun parseMeta_recycleBinDisabled() {
        val xml = wrapKeePassFile(
            meta = """
                <RecycleBinEnabled>False</RecycleBinEnabled>
            """,
        )
        val result = readerV3().readXml(xml)

        assertFalse(result.meta.recycleBinEnabled)
        assertNull(result.meta.recycleBinUuid)
    }

    @Test
    fun parseMeta_memoryProtection() {
        val xml = wrapKeePassFile(
            meta = """
                <MemoryProtection>
                    <ProtectTitle>True</ProtectTitle>
                    <ProtectUserName>True</ProtectUserName>
                    <ProtectPassword>True</ProtectPassword>
                    <ProtectURL>False</ProtectURL>
                    <ProtectNotes>False</ProtectNotes>
                </MemoryProtection>
            """,
        )
        val result = readerV3().readXml(xml)

        assertTrue(result.meta.protectTitle)
        assertTrue(result.meta.protectUserName)
        assertTrue(result.meta.protectPassword)
        assertFalse(result.meta.protectUrl)
        assertFalse(result.meta.protectNotes)
    }

    @Test
    fun parseMeta_historyMaxItems() {
        val xml = wrapKeePassFile(
            meta = """
                <HistoryMaxItems>20</HistoryMaxItems>
                <HistoryMaxSize>10485760</HistoryMaxSize>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals(20, result.meta.historyMaxItems)
        assertEquals(10485760L, result.meta.historyMaxSize)
    }

    @Test
    fun parseMeta_historyMaxItems_defaults() {
        val xml = wrapKeePassFile(meta = "")
        val result = readerV3().readXml(xml)

        assertEquals(12, result.meta.historyMaxItems)
        assertEquals(6291456L, result.meta.historyMaxSize)
    }

    @Test
    fun parseMeta_historyMaxItems_negativeUnlimited() {
        val xml = wrapKeePassFile(
            meta = """
                <HistoryMaxItems>-1</HistoryMaxItems>
                <HistoryMaxSize>-1</HistoryMaxSize>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals(-1, result.meta.historyMaxItems)
        assertEquals(-1L, result.meta.historyMaxSize)
    }

    // -- Group parsing tests --

    @Test
    fun parseGroup_basic() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Notes>Root group notes</Notes>
                    <IconID>48</IconID>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals("cm9vdA==", result.rootGroup.uuid)
        assertEquals("Root", result.rootGroup.name)
        assertEquals("Root group notes", result.rootGroup.notes)
        assertEquals(48, result.rootGroup.icon.standardIndex)
    }

    @Test
    fun parseGroup_nestedGroups() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Group>
                        <UUID>Y2hpbGQ=</UUID>
                        <Name>Internet</Name>
                        <IconID>1</IconID>
                        <Group>
                            <UUID>Z3JhbmQ=</UUID>
                            <Name>Social</Name>
                            <IconID>2</IconID>
                        </Group>
                    </Group>
                    <Group>
                        <UUID>ZW1haWw=</UUID>
                        <Name>Email</Name>
                        <IconID>3</IconID>
                    </Group>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals(2, result.rootGroup.groups.size)
        assertEquals("Internet", result.rootGroup.groups[0].name)
        assertEquals("Email", result.rootGroup.groups[1].name)

        // Nested grandchild
        assertEquals(1, result.rootGroup.groups[0].groups.size)
        assertEquals("Social", result.rootGroup.groups[0].groups[0].name)
    }

    // -- Entry parsing tests --

    @Test
    fun parseEntry_standardFields() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <IconID>0</IconID>
                        <String>
                            <Key>Title</Key>
                            <Value>My Website</Value>
                        </String>
                        <String>
                            <Key>UserName</Key>
                            <Value>user@example.com</Value>
                        </String>
                        <String>
                            <Key>Password</Key>
                            <Value>secret123</Value>
                        </String>
                        <String>
                            <Key>URL</Key>
                            <Value>https://example.com</Value>
                        </String>
                        <String>
                            <Key>Notes</Key>
                            <Value>Some notes here</Value>
                        </String>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)
        val entry = result.rootGroup.entries.first()

        assertEquals("ZW50cnk=", entry.uuid)
        assertEquals("My Website", entry.title)
        assertEquals("user@example.com", entry.userName)
        assertEquals("secret123", entry.password)
        assertEquals("https://example.com", entry.url)
        assertEquals("Some notes here", entry.notes)
        assertEquals(5, entry.fields.size)
    }

    @Test
    fun parseEntry_protectedField_withDecryptor() {
        // "secret123" encoded as bytes, then base64'd as fake "ciphertext"
        val plaintext = "secret123".encodeToByteArray()
        val ciphertext = Base64.encode(plaintext)

        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <String>
                            <Key>Password</Key>
                            <Value Protected="True">$ciphertext</Value>
                        </String>
                    </Entry>
                </Group>
            """,
        )

        // Identity decryptor: returns ciphertext as-is (since we used plaintext bytes as fake ciphertext)
        val decryptor = InnerStreamDecryptor { it }
        val result = KdbxXmlReader(isV4 = false, innerStreamDecryptor = decryptor).readXml(xml)
        val entry = result.rootGroup.entries.first()
        val field = entry.fields.first()

        assertTrue(field.isProtected)
        assertEquals("secret123", field.value)
    }

    @Test
    fun parseEntry_protectedField_withoutDecryptor() {
        val ciphertext = Base64.encode("encrypted".encodeToByteArray())

        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <String>
                            <Key>Password</Key>
                            <Value Protected="True">$ciphertext</Value>
                        </String>
                    </Entry>
                </Group>
            """,
        )

        // No decryptor: value should remain as raw base64
        val result = readerV3().readXml(xml)
        val field = result.rootGroup.entries.first().fields.first()

        assertTrue(field.isProtected)
        assertEquals(ciphertext, field.value)
    }

    @Test
    fun parseEntry_tags() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <Tags>banking;finance;important</Tags>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)
        val entry = result.rootGroup.entries.first()

        assertEquals(listOf("banking", "finance", "important"), entry.tags)
    }

    @Test
    fun parseEntry_emptyTags() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <Tags></Tags>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)

        assertTrue(result.rootGroup.entries.first().tags.isEmpty())
    }

    @Test
    fun parseEntry_times_v3() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <Times>
                            <CreationTime>2024-06-15T10:30:00Z</CreationTime>
                            <LastModificationTime>2024-06-16T12:00:00Z</LastModificationTime>
                            <LastAccessTime>2024-06-17T08:00:00Z</LastAccessTime>
                            <ExpiryTime>2025-06-15T10:30:00Z</ExpiryTime>
                            <Expires>True</Expires>
                        </Times>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)
        val entry = result.rootGroup.entries.first()

        assertEquals(Instant.parse("2024-06-15T10:30:00Z"), entry.creationTime)
        assertEquals(Instant.parse("2024-06-16T12:00:00Z"), entry.lastModificationTime)
        assertEquals(Instant.parse("2024-06-17T08:00:00Z"), entry.lastAccessTime)
        assertEquals(Instant.parse("2025-06-15T10:30:00Z"), entry.expiryTime)
        assertTrue(entry.expires)
    }

    @Test
    fun parseEntry_times_v4_base64() {
        // Encode a known Unix timestamp as KDBX 4.x base64 timestamp
        // 2024-01-01T00:00:00Z = Unix 1704067200 + 62135596800 = 63839664000
        val seconds = 1704067200L + 62135596800L
        val encoded = encodeV4Timestamp(seconds)

        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <Times>
                            <CreationTime>$encoded</CreationTime>
                            <Expires>False</Expires>
                        </Times>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV4().readXml(xml)
        val entry = result.rootGroup.entries.first()

        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), entry.creationTime)
        assertFalse(entry.expires)
    }

    @Test
    fun parseEntry_history() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <String>
                            <Key>Title</Key>
                            <Value>Current Title</Value>
                        </String>
                        <History>
                            <Entry>
                                <UUID>ZW50cnk=</UUID>
                                <String>
                                    <Key>Title</Key>
                                    <Value>Old Title</Value>
                                </String>
                            </Entry>
                        </History>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)
        val entry = result.rootGroup.entries.first()

        assertEquals("Current Title", entry.title)
        assertEquals(1, entry.history.size)
        assertEquals("Old Title", entry.history[0].title)
    }

    @Test
    fun parseEntry_customIcon() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <IconID>5</IconID>
                        <CustomIconUUID>Y3VzdG9t</CustomIconUUID>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)
        val entry = result.rootGroup.entries.first()

        assertEquals(5, entry.icon.standardIndex)
        assertEquals("Y3VzdG9t", entry.icon.customUuid)
    }

    // -- Binary/Attachment tests --

    @Test
    fun parseBinaries_andEntryReference() {
        val binaryData = "Hello, World!".encodeToByteArray()
        val base64Binary = Base64.encode(binaryData)

        val xml = wrapKeePassFile(
            meta = """
                <Binaries>
                    <Binary ID="0">$base64Binary</Binary>
                </Binaries>
            """,
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <Binary>
                            <Key>readme.txt</Key>
                            <Value Ref="0" />
                        </Binary>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)

        // Check binaries pool
        assertEquals(1, result.binaryPool.size)
        assertTrue(binaryData.contentEquals(result.binaryPool[0]!!))

        // Check entry attachment
        val attachment = result.rootGroup.entries.first().attachments.first()
        assertEquals("readme.txt", attachment.name)
        assertEquals(0, attachment.id)
        assertTrue(binaryData.contentEquals(attachment.data))
    }

    // -- DeletedObjects tests --

    @Test
    fun parseDeletedObjects() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                </Group>
                <DeletedObjects>
                    <DeletedObject>
                        <UUID>ZGVsZXRlZA==</UUID>
                        <DeletionTime>2024-03-15T14:00:00Z</DeletionTime>
                    </DeletedObject>
                    <DeletedObject>
                        <UUID>ZGVsZXRlZDI=</UUID>
                        <DeletionTime>2024-03-16T09:30:00Z</DeletionTime>
                    </DeletedObject>
                </DeletedObjects>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals(2, result.deletedObjects.size)
        assertEquals("ZGVsZXRlZA==", result.deletedObjects[0].uuid)
        assertEquals(Instant.parse("2024-03-15T14:00:00Z"), result.deletedObjects[0].deletionTime)
        assertEquals("ZGVsZXRlZDI=", result.deletedObjects[1].uuid)
        assertEquals(Instant.parse("2024-03-16T09:30:00Z"), result.deletedObjects[1].deletionTime)
    }

    @Test
    fun parseDeletedObjects_empty() {
        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                </Group>
                <DeletedObjects>
                </DeletedObjects>
            """,
        )
        val result = readerV3().readXml(xml)

        assertTrue(result.deletedObjects.isEmpty())
    }

    // -- Unknown elements are skipped --

    @Test
    fun unknownElements_skipped() {
        val xml = wrapKeePassFile(
            meta = """
                <Generator>KeePass</Generator>
                <DatabaseName>Test</DatabaseName>
                <UnknownElement>
                    <Deeply><Nested>Content</Nested></Deeply>
                </UnknownElement>
            """,
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <UnknownGroupChild>foo</UnknownGroupChild>
                    <Entry>
                        <UUID>ZW50cnk=</UUID>
                        <UnknownEntryChild>bar</UnknownEntryChild>
                    </Entry>
                </Group>
            """,
        )
        val result = readerV3().readXml(xml)

        assertEquals("Test", result.meta.databaseName)
        assertEquals("Root", result.rootGroup.name)
        assertEquals(1, result.rootGroup.entries.size)
    }

    // -- Full integration test --

    @Test
    fun fullXml_integration() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <KeePassFile>
                <Meta>
                    <Generator>KeePass</Generator>
                    <DatabaseName>My Passwords</DatabaseName>
                    <DatabaseDescription>Personal password database</DatabaseDescription>
                    <DefaultUserName>defaultuser</DefaultUserName>
                    <RecycleBinEnabled>True</RecycleBinEnabled>
                    <RecycleBinUUID>cmVjeWNsZQ==</RecycleBinUUID>
                    <MemoryProtection>
                        <ProtectTitle>False</ProtectTitle>
                        <ProtectUserName>False</ProtectUserName>
                        <ProtectPassword>True</ProtectPassword>
                        <ProtectURL>False</ProtectURL>
                        <ProtectNotes>False</ProtectNotes>
                    </MemoryProtection>
                </Meta>
                <Root>
                    <Group>
                        <UUID>cm9vdA==</UUID>
                        <Name>Root</Name>
                        <IconID>48</IconID>
                        <Group>
                            <UUID>aW50ZXJuZXQ=</UUID>
                            <Name>Internet</Name>
                            <IconID>1</IconID>
                            <Entry>
                                <UUID>Z21haWw=</UUID>
                                <IconID>0</IconID>
                                <Tags>email;google</Tags>
                                <Times>
                                    <CreationTime>2024-01-15T10:00:00Z</CreationTime>
                                    <LastModificationTime>2024-06-01T12:00:00Z</LastModificationTime>
                                    <Expires>False</Expires>
                                </Times>
                                <String>
                                    <Key>Title</Key>
                                    <Value>Gmail</Value>
                                </String>
                                <String>
                                    <Key>UserName</Key>
                                    <Value>user@gmail.com</Value>
                                </String>
                                <String>
                                    <Key>Password</Key>
                                    <Value>mypassword</Value>
                                </String>
                                <String>
                                    <Key>URL</Key>
                                    <Value>https://mail.google.com</Value>
                                </String>
                            </Entry>
                        </Group>
                        <Group>
                            <UUID>YmFua2luZw==</UUID>
                            <Name>Banking</Name>
                            <IconID>37</IconID>
                        </Group>
                    </Group>
                    <DeletedObjects>
                        <DeletedObject>
                            <UUID>b2xkZW50cnk=</UUID>
                            <DeletionTime>2024-05-20T08:00:00Z</DeletionTime>
                        </DeletedObject>
                    </DeletedObjects>
                </Root>
            </KeePassFile>
        """.trimIndent()

        val result = readerV3().readXml(xml)

        // Meta
        assertEquals("My Passwords", result.meta.databaseName)
        assertEquals("Personal password database", result.meta.description)
        assertEquals("defaultuser", result.meta.defaultUserName)
        assertTrue(result.meta.recycleBinEnabled)
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
    }

    // -- Multiple entries with protected fields: decryptor called in order --

    @Test
    fun protectedFields_decryptedInOrder() {
        val passwords = listOf("pass1", "pass2", "pass3")
        val encoded = passwords.map { Base64.encode(it.encodeToByteArray()) }

        val xml = wrapKeePassFile(
            root = """
                <Group>
                    <UUID>cm9vdA==</UUID>
                    <Name>Root</Name>
                    <Entry>
                        <UUID>ZTE=</UUID>
                        <String><Key>Password</Key><Value Protected="True">${encoded[0]}</Value></String>
                    </Entry>
                    <Entry>
                        <UUID>ZTI=</UUID>
                        <String><Key>Password</Key><Value Protected="True">${encoded[1]}</Value></String>
                    </Entry>
                    <Entry>
                        <UUID>ZTM=</UUID>
                        <String><Key>Password</Key><Value Protected="True">${encoded[2]}</Value></String>
                    </Entry>
                </Group>
            """,
        )

        // Track decryption order
        val decryptionOrder = mutableListOf<String>()
        val decryptor = InnerStreamDecryptor { ciphertext ->
            val text = ciphertext.decodeToString()
            decryptionOrder.add(text)
            ciphertext // identity decryptor
        }

        val result = KdbxXmlReader(isV4 = false, innerStreamDecryptor = decryptor).readXml(xml)
        val entries = result.rootGroup.entries

        assertEquals(3, entries.size)
        assertEquals("pass1", entries[0].password)
        assertEquals("pass2", entries[1].password)
        assertEquals("pass3", entries[2].password)

        // Verify decryption was called in document order
        assertEquals(listOf("pass1", "pass2", "pass3"), decryptionOrder)
    }

    // -- Helpers --

    private fun readerV3() = KdbxXmlReader(isV4 = false)
    private fun readerV4() = KdbxXmlReader(isV4 = true)

    private fun wrapKeePassFile(
        meta: String = "",
        root: String = """
            <Group>
                <UUID>cm9vdA==</UUID>
                <Name>Root</Name>
            </Group>
        """,
    ): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <KeePassFile>
            <Meta>$meta</Meta>
            <Root>$root</Root>
        </KeePassFile>
    """.trimIndent()

    private fun encodeV4Timestamp(secondsSinceDotNetEpoch: Long): String {
        val bytes = ByteArray(8)
        var value = secondsSinceDotNetEpoch
        for (i in 0 until 8) {
            bytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }
        return Base64.encode(bytes)
    }
}
