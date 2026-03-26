package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.crypto.PlatformCryptoProvider
import org.github.keepasscompose.core.model.Argon2Variant
import org.github.keepasscompose.core.model.CipherId
import org.github.keepasscompose.core.model.CompositeKey
import org.github.keepasscompose.core.model.CompressionAlgorithm
import org.github.keepasscompose.core.model.InnerStreamCipher
import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxDatabase
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxHeader
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta
import org.github.keepasscompose.core.model.KdbxVersion
import org.github.keepasscompose.core.model.KdfParameters
import kotlin.time.Instant

/**
 * Generates sample KDBX databases for use as test fixtures.
 * All fixtures use deterministic seeds/IVs for reproducibility.
 */
object KdbxFixtureGenerator {
    private val crypto = PlatformCryptoProvider()
    private val writer = KdbxWriter(crypto)

    /** Password used for all V4 fixtures. */
    const val V4_PASSWORD = "fixture-v4-password"

    /** Password used for all V3 fixtures. */
    const val V3_PASSWORD = "fixture-v3-password"

    /** Generates a KDBX 4.x file with AES-256 cipher and Argon2d KDF. */
    fun generateV4AesArgon2d(): ByteArray = writer.writeDatabase(
        buildV4Database(
            databaseName = "V4 AES Argon2d Fixture",
            cipher = CipherId.AES_256,
            kdf =
            KdfParameters.Argon2(
                variant = Argon2Variant.ARGON2D,
                salt = ByteArray(32) { (it + 1).toByte() },
                parallelism = 2,
                memory = 1024,
                iterations = 1,
            ),
        ),
        CompositeKey(password = V4_PASSWORD),
    )

    /** Generates a KDBX 4.x file with ChaCha20 cipher and AES-KDF. */
    fun generateV4ChaCha20(): ByteArray = writer.writeDatabase(
        buildV4Database(
            databaseName = "V4 ChaCha20 Fixture",
            cipher = CipherId.CHACHA20,
            kdf =
            KdfParameters.AesKdf(
                rounds = 2,
                seed = ByteArray(32) { (it + 2).toByte() },
            ),
        ),
        CompositeKey(password = V4_PASSWORD),
    )

    /** Generates a KDBX 4.x file with Twofish cipher. */
    fun generateV4Twofish(): ByteArray = writer.writeDatabase(
        buildV4Database(
            databaseName = "V4 Twofish Fixture",
            cipher = CipherId.TWOFISH,
            kdf =
            KdfParameters.AesKdf(
                rounds = 2,
                seed = ByteArray(32) { (it + 3).toByte() },
            ),
        ),
        CompositeKey(password = V4_PASSWORD),
    )

    /** Generates a KDBX 3.1 file with AES cipher and Salsa20 inner stream. */
    fun generateV3Aes(): ByteArray = writer.writeDatabase(
        buildV3Database(databaseName = "V3 AES Fixture"),
        CompositeKey(password = V3_PASSWORD),
    )

    /** Generates a KDBX 3.1 file with Twofish cipher. */
    fun generateV3Twofish(): ByteArray = writer.writeDatabase(
        buildV3Database(
            databaseName = "V3 Twofish Fixture",
            cipher = CipherId.TWOFISH,
        ),
        CompositeKey(password = V3_PASSWORD),
    )

    private fun buildV4Database(databaseName: String, cipher: CipherId, kdf: KdfParameters): KdbxDatabase = KdbxDatabase(
        meta = buildMeta(databaseName),
        header =
        KdbxHeader(
            version = KdbxVersion(4, 0),
            cipher = cipher,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { (it + 0x10).toByte() },
            encryptionIv =
            if (cipher == CipherId.CHACHA20) {
                ByteArray(12) { (it + 0x20).toByte() }
            } else {
                ByteArray(16) { (it + 0x20).toByte() }
            },
            kdfParameters = kdf,
            innerRandomStreamId = InnerStreamCipher.CHACHA20,
            innerRandomStreamKey = ByteArray(64) { (it + 0x30).toByte() },
        ),
        rootGroup = buildSampleRootGroup(),
    )

    private fun buildV3Database(databaseName: String, cipher: CipherId = CipherId.AES_256): KdbxDatabase = KdbxDatabase(
        meta = buildMeta(databaseName),
        header =
        KdbxHeader(
            version = KdbxVersion(3, 1),
            cipher = cipher,
            compression = CompressionAlgorithm.GZIP,
            masterSeed = ByteArray(32) { (it + 0x10).toByte() },
            encryptionIv = ByteArray(16) { (it + 0x20).toByte() },
            kdfParameters =
            KdfParameters.AesKdf(
                rounds = 2,
                seed = ByteArray(32) { (it + 0x40).toByte() },
            ),
            innerRandomStreamId = InnerStreamCipher.SALSA20,
            innerRandomStreamKey = ByteArray(32) { (it + 0x50).toByte() },
            streamStartBytes = ByteArray(32) { (it + 0x60).toByte() },
        ),
        rootGroup = buildSampleRootGroup(),
    )

    private fun buildMeta(databaseName: String) = KdbxMeta(
        databaseName = databaseName,
        description = "Test fixture database",
        defaultUserName = "testuser",
        historyMaxItems = 10,
        historyMaxSize = 6_291_456,
        recycleBinEnabled = true,
    )

    /** Builds a sample root group with nested groups and multiple entries. */
    private fun buildSampleRootGroup() = KdbxGroup(
        uuid = "fixture-root",
        name = "Root",
        entries =
        listOf(
            KdbxEntry(
                uuid = "entry-email",
                fields =
                listOf(
                    KdbxEntryField("Title", "Email Account"),
                    KdbxEntryField("UserName", "user@example.com"),
                    KdbxEntryField("Password", "email-p@ssw0rd!", isProtected = true),
                    KdbxEntryField("URL", "https://mail.example.com"),
                    KdbxEntryField("Notes", "Primary email account"),
                ),
                tags = listOf("email", "personal"),
                creationTime = Instant.parse("2024-01-15T10:00:00Z"),
                lastModificationTime = Instant.parse("2024-06-01T12:00:00Z"),
            ),
        ),
        groups =
        listOf(
            KdbxGroup(
                uuid = "group-banking",
                name = "Banking",
                icon = KdbxIcon(standardIndex = 37),
                entries =
                listOf(
                    KdbxEntry(
                        uuid = "entry-bank",
                        fields =
                        listOf(
                            KdbxEntryField("Title", "Bank Account"),
                            KdbxEntryField("UserName", "bankuser"),
                            KdbxEntryField("Password", "b@nk-s3cure!", isProtected = true),
                            KdbxEntryField("URL", "https://bank.example.com"),
                        ),
                        creationTime = Instant.parse("2024-02-01T08:00:00Z"),
                        lastModificationTime = Instant.parse("2024-05-15T09:00:00Z"),
                    ),
                ),
            ),
            KdbxGroup(
                uuid = "group-work",
                name = "Work",
                icon = KdbxIcon(standardIndex = 38),
                entries =
                listOf(
                    KdbxEntry(
                        uuid = "entry-vpn",
                        fields =
                        listOf(
                            KdbxEntryField("Title", "VPN Access"),
                            KdbxEntryField("UserName", "vpnuser"),
                            KdbxEntryField("Password", "vpn-k3y!", isProtected = true),
                        ),
                    ),
                ),
            ),
        ),
    )
}
