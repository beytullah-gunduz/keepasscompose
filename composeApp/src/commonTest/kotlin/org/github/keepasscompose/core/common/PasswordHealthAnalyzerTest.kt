package org.github.keepasscompose.core.common

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class PasswordHealthAnalyzerTest {
    private fun entry(title: String, password: String, expires: Boolean = false, expiryTime: Instant? = null): KdbxEntry = KdbxEntry(
        uuid = title,
        fields =
        listOf(
            KdbxEntryField("Title", title),
            KdbxEntryField("UserName", "user"),
            KdbxEntryField("Password", password, isProtected = true),
            KdbxEntryField("URL", ""),
            KdbxEntryField("Notes", ""),
        ),
        expires = expires,
        expiryTime = expiryTime,
    )

    private val now = Instant.fromEpochSeconds(1700000000) // 2023-11-14

    // --- Basic analysis ---

    @Test
    fun analyze_countsEntries() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries = listOf(entry("A", "Str0ngP@ss!"), entry("B", "x7Km#9pQ")),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(2, report.totalEntries)
        assertEquals(2, report.entriesWithPasswords)
    }

    @Test
    fun analyze_emptyDatabase() {
        val root = KdbxGroup(uuid = "root", name = "Root")
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(0, report.totalEntries)
        assertEquals(0, report.entriesWithPasswords)
        assertTrue(report.weakPasswords.isEmpty())
        assertTrue(report.reusedPasswords.isEmpty())
    }

    // --- Weak passwords ---

    @Test
    fun analyze_detectsWeakPasswords() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries =
                listOf(
                    entry("Weak", "123"),
                    entry("Strong", "k8#Lm2!qR5@vN7&pX3\$wZ9*cF4^bY6+"),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(1, report.weakPasswords.size)
        assertEquals("Weak", report.weakPasswords[0].entry.title)
    }

    @Test
    fun analyze_emptyPasswordNotCountedAsWeak() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries = listOf(entry("NoPass", "")),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(0, report.entriesWithPasswords)
        assertTrue(report.weakPasswords.isEmpty())
    }

    // --- Reused passwords ---

    @Test
    fun analyze_detectsReusedPasswords() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries =
                listOf(
                    entry("Site1", "samepassword"),
                    entry("Site2", "samepassword"),
                    entry("Site3", "differentpass"),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(1, report.reusedPasswords.size)
        val reusedGroup = report.reusedPasswords.values.first()
        assertEquals(2, reusedGroup.size)

        val site1 = report.entries.first { it.entry.title == "Site1" }
        assertTrue(site1.isReused)
        assertEquals(2, site1.reusedCount)

        val site3 = report.entries.first { it.entry.title == "Site3" }
        assertFalse(site3.isReused)
    }

    @Test
    fun analyze_tripleReuse() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries =
                listOf(
                    entry("A", "shared"),
                    entry("B", "shared"),
                    entry("C", "shared"),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(1, report.reusedPasswords.size)
        assertEquals(
            3,
            report.reusedPasswords.values
                .first()
                .size,
        )
    }

    // --- Expired entries ---

    @Test
    fun analyze_detectsExpiredEntries() {
        val pastTime = Instant.fromEpochSeconds(1600000000) // 2020
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries =
                listOf(
                    entry("Expired", "pass123!Xyz", expires = true, expiryTime = pastTime),
                    entry("NotExpired", "pass456!Abc"),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(1, report.expiredEntries.size)
        assertEquals("Expired", report.expiredEntries[0].entry.title)
    }

    @Test
    fun analyze_expiresDisabled_notExpired() {
        val pastTime = Instant.fromEpochSeconds(1600000000)
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries = listOf(entry("NotTracked", "pass!Xyz", expires = false, expiryTime = pastTime)),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertTrue(report.expiredEntries.isEmpty())
    }

    // --- Expiring soon ---

    @Test
    fun analyze_detectsExpiringSoon() {
        val soonTime = now + 15.days
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries =
                listOf(
                    entry("ExpiringSoon", "pass!Xyz123", expires = true, expiryTime = soonTime),
                    entry("FarFuture", "pass!Abc456", expires = true, expiryTime = now + 90.days),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now, expiringSoonThreshold = 30.days)
        assertEquals(1, report.expiringSoonEntries.size)
        assertEquals("ExpiringSoon", report.expiringSoonEntries[0].entry.title)
    }

    // --- Subgroups ---

    @Test
    fun analyze_searchesSubgroups() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries = listOf(entry("Top", "pass!Top123")),
                groups =
                listOf(
                    KdbxGroup(
                        uuid = "sub",
                        name = "Sub",
                        entries = listOf(entry("Nested", "123")),
                    ),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertEquals(2, report.totalEntries)
        val nested = report.entries.first { it.entry.title == "Nested" }
        assertEquals(listOf("Root", "Sub"), nested.groupPath)
    }

    // --- Average strength ---

    @Test
    fun analyze_averageStrength() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries =
                listOf(
                    entry("Strong1", "k8#Lm2!qR5@vN7&pX3\$wZ9*cF4^bY6+"),
                    entry("Strong2", "p9@Qn4!xT7#mK2\$wR5&jL8*vF3^cY1+"),
                ),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        assertTrue(report.averageStrength >= PasswordStrength.Level.STRONG)
    }

    // --- EntryHealth fields ---

    @Test
    fun entryHealth_strengthPopulated() {
        val root =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries = listOf(entry("Test", "MyP@ssw0rd!")),
            )
        val report = PasswordHealthAnalyzer.analyze(root, now)
        val health = report.entries[0]
        assertTrue(health.strength.entropyBits > 0)
        assertTrue(health.strength.crackTimeDisplay.isNotEmpty())
    }
}
