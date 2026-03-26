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
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Generates sample.kdbx in commonTest/resources for use by cross-platform tests.
 *
 * Password: sample123
 * Structure:
 *   Root/
 *     - GitHub (dev@example.com)
 *     - Google Account (user@gmail.com)
 *     - Home WiFi
 *     Admin/
 *       - Production Server (root)
 *       - Database Admin (db_admin)
 *       - AWS Console (admin@company.com)
 */
class GenerateSampleFixture {
    @Test
    fun generateSampleKdbx() {
        val crypto = PlatformCryptoProvider()
        val writer = KdbxWriter(crypto)

        val db = KdbxDatabase(
            meta = KdbxMeta(
                databaseName = "Sample Database",
                description = "Sample KDBX for testing",
                defaultUserName = "",
                historyMaxItems = 10,
                historyMaxSize = 6_291_456,
                recycleBinEnabled = false,
            ),
            header = KdbxHeader(
                version = KdbxVersion(4, 0),
                cipher = CipherId.AES_256,
                compression = CompressionAlgorithm.GZIP,
                masterSeed = ByteArray(32) { (it + 0xAA).toByte() },
                encryptionIv = ByteArray(16) { (it + 0xBB).toByte() },
                kdfParameters = KdfParameters.Argon2(
                    variant = Argon2Variant.ARGON2D,
                    salt = ByteArray(32) { (it + 0xCC).toByte() },
                    parallelism = 2,
                    memory = 1024,
                    iterations = 1,
                ),
                innerRandomStreamId = InnerStreamCipher.CHACHA20,
                innerRandomStreamKey = ByteArray(64) { (it + 0xDD).toByte() },
            ),
            rootGroup = buildSampleRootGroup(),
        )

        val bytes = writer.writeDatabase(db, CompositeKey(password = SAMPLE_PASSWORD))

        val outputFile = File("src/commonTest/resources/sample.kdbx")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(bytes)
        assertTrue(outputFile.exists())
        println("Generated sample.kdbx (${bytes.size} bytes)")
    }

    companion object {
        const val SAMPLE_PASSWORD = "sample123"

        fun buildSampleRootGroup() = KdbxGroup(
            uuid = "sample-root",
            name = "Root",
            entries = listOf(
                KdbxEntry(
                    uuid = "entry-github",
                    fields = listOf(
                        KdbxEntryField("Title", "GitHub"),
                        KdbxEntryField("UserName", "dev@example.com"),
                        KdbxEntryField("Password", "gh-t0ken-s3cret!", isProtected = true),
                        KdbxEntryField("URL", "https://github.com"),
                        KdbxEntryField("Notes", "Personal GitHub account"),
                    ),
                    tags = listOf("dev", "code"),
                    creationTime = Instant.parse("2024-03-01T10:00:00Z"),
                ),
                KdbxEntry(
                    uuid = "entry-google",
                    fields = listOf(
                        KdbxEntryField("Title", "Google Account"),
                        KdbxEntryField("UserName", "user@gmail.com"),
                        KdbxEntryField("Password", "g00gle-p@ss!", isProtected = true),
                        KdbxEntryField("URL", "https://accounts.google.com"),
                    ),
                    creationTime = Instant.parse("2024-03-15T08:00:00Z"),
                ),
                KdbxEntry(
                    uuid = "entry-wifi",
                    fields = listOf(
                        KdbxEntryField("Title", "Home WiFi"),
                        KdbxEntryField("UserName", ""),
                        KdbxEntryField("Password", "wifi-netw0rk-key", isProtected = true),
                        KdbxEntryField("Notes", "Router: 192.168.1.1"),
                    ),
                    creationTime = Instant.parse("2024-04-01T12:00:00Z"),
                ),
            ),
            groups = listOf(
                KdbxGroup(
                    uuid = "group-admin",
                    name = "Admin",
                    icon = KdbxIcon(standardIndex = 68),
                    entries = listOf(
                        KdbxEntry(
                            uuid = "entry-server-root",
                            fields = listOf(
                                KdbxEntryField("Title", "Production Server"),
                                KdbxEntryField("UserName", "root"),
                                KdbxEntryField("Password", "pr0d-r00t-2024!", isProtected = true),
                                KdbxEntryField("URL", "ssh://prod.example.com"),
                                KdbxEntryField("Notes", "SSH port 2222"),
                            ),
                            tags = listOf("server", "admin"),
                            creationTime = Instant.parse("2024-02-10T09:00:00Z"),
                        ),
                        KdbxEntry(
                            uuid = "entry-db-admin",
                            fields = listOf(
                                KdbxEntryField("Title", "Database Admin"),
                                KdbxEntryField("UserName", "db_admin"),
                                KdbxEntryField("Password", "db-@dmin-s3cure!", isProtected = true),
                                KdbxEntryField("URL", "https://db.example.com:5432"),
                            ),
                            creationTime = Instant.parse("2024-02-15T11:00:00Z"),
                        ),
                        KdbxEntry(
                            uuid = "entry-aws",
                            fields = listOf(
                                KdbxEntryField("Title", "AWS Console"),
                                KdbxEntryField("UserName", "admin@company.com"),
                                KdbxEntryField("Password", "aws-c0nsole-2024!", isProtected = true),
                                KdbxEntryField("URL", "https://console.aws.amazon.com"),
                            ),
                            tags = listOf("cloud", "admin"),
                            creationTime = Instant.parse("2024-03-20T14:00:00Z"),
                        ),
                    ),
                ),
            ),
        )
    }
}
