package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxDatabase
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Generates sample-groups.kdbx in commonTest/resources: a database with several folders,
 * each holding several entries, for tests and manual checks in KeePass clients.
 *
 * Password: sample
 * Structure:
 *   Root/
 *     Personal/
 *       - Personal Email (jane.doe@example.com)
 *       - Streaming (jane.doe)
 *       - Online Shopping (jane.doe@example.com)
 *     Work/
 *       - Work Email (j.doe@company.example)
 *       - Corporate VPN (jdoe)
 *       - Issue Tracker (jdoe)
 *       Servers/
 *         - Production Server (root)
 *         - Staging Server (deploy)
 *     Finance/
 *       - Online Banking (12345678)
 *       - Credit Card (Jane Doe)
 *       - Tax Portal (jane.doe)
 *     Social/
 *       - Forum (janedoe)
 *       - Photo Sharing (jane_doe)
 *       - Professional Network (jane.doe@example.com)
 */
class GenerateGroupedSampleFixture {
    @Test
    fun generateGroupedSampleKdbx() {
        val crypto = PlatformCryptoProvider()
        val writer = KdbxWriter(crypto)

        val db = KdbxDatabase(
            meta = KdbxMeta(
                databaseName = "Grouped Sample Database",
                description = "Sample KDBX with several folders for testing",
                defaultUserName = "",
                historyMaxItems = 10,
                historyMaxSize = 6_291_456,
                recycleBinEnabled = false,
            ),
            header = KdbxHeader(
                version = KdbxVersion(4, 0),
                cipher = CipherId.AES_256,
                compression = CompressionAlgorithm.GZIP,
                masterSeed = ByteArray(32) { (it + 0x11).toByte() },
                encryptionIv = ByteArray(16) { (it + 0x22).toByte() },
                kdfParameters = KdfParameters.Argon2(
                    variant = Argon2Variant.ARGON2D,
                    salt = ByteArray(32) { (it + 0x33).toByte() },
                    parallelism = 2,
                    memory = 1024,
                    iterations = 2,
                ),
                innerRandomStreamId = InnerStreamCipher.CHACHA20,
                innerRandomStreamKey = ByteArray(64) { (it + 0x44).toByte() },
            ),
            rootGroup = buildGroupedSampleRootGroup(),
        )

        val bytes = writer.writeDatabase(db, CompositeKey(password = GROUPED_SAMPLE_PASSWORD))

        val outputFile = File("src/commonTest/resources/sample-groups.kdbx")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(bytes)
        assertTrue(outputFile.exists())
        println("Generated sample-groups.kdbx (${bytes.size} bytes)")
    }

    companion object {
        const val GROUPED_SAMPLE_PASSWORD = "sample"

        /** Deterministic, valid KDBX UUID (16 bytes, Base64) so other KeePass clients accept the file. */
        private fun fixtureUuid(index: Int): String = Base64.encode(ByteArray(16) { (index * 16 + it).toByte() })

        private fun entry(
            index: Int,
            title: String,
            userName: String,
            password: String,
            url: String = "",
            notes: String = "",
            tags: List<String> = emptyList(),
            created: String,
        ) = KdbxEntry(
            uuid = fixtureUuid(index),
            fields = listOf(
                KdbxEntryField("Title", title),
                KdbxEntryField("UserName", userName),
                KdbxEntryField("Password", password, isProtected = true),
                KdbxEntryField("URL", url),
                KdbxEntryField("Notes", notes),
            ),
            tags = tags,
            creationTime = Instant.parse(created),
        )

        fun buildGroupedSampleRootGroup() = KdbxGroup(
            uuid = fixtureUuid(0),
            name = "Root",
            groups = listOf(
                KdbxGroup(
                    uuid = fixtureUuid(1),
                    name = "Personal",
                    icon = KdbxIcon(standardIndex = 1),
                    entries = listOf(
                        entry(
                            index = 10,
                            title = "Personal Email",
                            userName = "jane.doe@example.com",
                            password = "m@il-p3rs0nal!",
                            url = "https://mail.example.com",
                            tags = listOf("email"),
                            created = "2024-01-05T09:00:00Z",
                        ),
                        entry(
                            index = 11,
                            title = "Streaming",
                            userName = "jane.doe",
                            password = "str3am-n1ght$",
                            url = "https://streaming.example.com",
                            created = "2024-01-12T20:30:00Z",
                        ),
                        entry(
                            index = 12,
                            title = "Online Shopping",
                            userName = "jane.doe@example.com",
                            password = "sh0p-c@rt-2024",
                            url = "https://shop.example.com",
                            notes = "Prime member until 2025-06",
                            created = "2024-02-02T14:15:00Z",
                        ),
                    ),
                ),
                KdbxGroup(
                    uuid = fixtureUuid(2),
                    name = "Work",
                    icon = KdbxIcon(standardIndex = 48),
                    entries = listOf(
                        entry(
                            index = 20,
                            title = "Work Email",
                            userName = "j.doe@company.example",
                            password = "w0rk-m@il-Q1!",
                            url = "https://mail.company.example",
                            tags = listOf("email", "work"),
                            created = "2024-01-08T08:00:00Z",
                        ),
                        entry(
                            index = 21,
                            title = "Corporate VPN",
                            userName = "jdoe",
                            password = "vpn-t0kEn-77",
                            url = "https://vpn.company.example",
                            notes = "Use the EU gateway",
                            tags = listOf("work"),
                            created = "2024-01-08T08:10:00Z",
                        ),
                        entry(
                            index = 22,
                            title = "Issue Tracker",
                            userName = "jdoe",
                            password = "tr@cker-s3cret",
                            url = "https://issues.company.example",
                            tags = listOf("work"),
                            created = "2024-01-09T10:00:00Z",
                        ),
                    ),
                    groups = listOf(
                        KdbxGroup(
                            uuid = fixtureUuid(3),
                            name = "Servers",
                            icon = KdbxIcon(standardIndex = 27),
                            entries = listOf(
                                entry(
                                    index = 30,
                                    title = "Production Server",
                                    userName = "root",
                                    password = "pr0d-r00t-x9!",
                                    url = "ssh://prod.company.example",
                                    notes = "SSH port 2222",
                                    tags = listOf("server", "admin"),
                                    created = "2024-02-10T09:00:00Z",
                                ),
                                entry(
                                    index = 31,
                                    title = "Staging Server",
                                    userName = "deploy",
                                    password = "st@ging-d3ploy",
                                    url = "ssh://staging.company.example",
                                    tags = listOf("server"),
                                    created = "2024-02-11T09:00:00Z",
                                ),
                            ),
                        ),
                    ),
                ),
                KdbxGroup(
                    uuid = fixtureUuid(4),
                    name = "Finance",
                    icon = KdbxIcon(standardIndex = 66),
                    entries = listOf(
                        entry(
                            index = 40,
                            title = "Online Banking",
                            userName = "12345678",
                            password = "b@nk-0nline-55",
                            url = "https://bank.example.com",
                            tags = listOf("finance"),
                            created = "2024-03-01T07:45:00Z",
                        ),
                        entry(
                            index = 41,
                            title = "Credit Card",
                            userName = "Jane Doe",
                            password = "0000",
                            notes = "Sample card, not a real number",
                            tags = listOf("finance"),
                            created = "2024-03-01T07:50:00Z",
                        ),
                        entry(
                            index = 42,
                            title = "Tax Portal",
                            userName = "jane.doe",
                            password = "t@x-p0rtal-24",
                            url = "https://tax.example.gov",
                            created = "2024-04-10T18:00:00Z",
                        ),
                    ),
                ),
                KdbxGroup(
                    uuid = fixtureUuid(5),
                    name = "Social",
                    icon = KdbxIcon(standardIndex = 59),
                    entries = listOf(
                        entry(
                            index = 50,
                            title = "Forum",
                            userName = "janedoe",
                            password = "f0rum-p0st3r",
                            url = "https://forum.example.org",
                            created = "2024-05-03T21:00:00Z",
                        ),
                        entry(
                            index = 51,
                            title = "Photo Sharing",
                            userName = "jane_doe",
                            password = "ph0t0-sn@p!",
                            url = "https://photos.example.com",
                            created = "2024-05-04T12:00:00Z",
                        ),
                        entry(
                            index = 52,
                            title = "Professional Network",
                            userName = "jane.doe@example.com",
                            password = "pr0-netw0rk#",
                            url = "https://network.example.com",
                            tags = listOf("work"),
                            created = "2024-05-05T09:30:00Z",
                        ),
                    ),
                ),
            ),
        )
    }
}
