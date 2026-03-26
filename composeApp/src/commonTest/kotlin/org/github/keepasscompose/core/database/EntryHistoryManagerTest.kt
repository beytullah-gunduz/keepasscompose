package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.KdbxAttachment
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class EntryHistoryManagerTest {
    // -- recordModification --

    @Test
    fun recordModification_addsPreviousVersionToHistory() {
        val manager = EntryHistoryManager(historyMaxItems = 10, historyMaxSize = -1)

        val previous = entry("e1", "Old Title", modTime = time("2024-01-01T00:00:00Z"))
        val current = entry("e1", "New Title", modTime = time("2024-02-01T00:00:00Z"))

        val result = manager.recordModification(current, previous)

        assertEquals(1, result.history.size)
        assertEquals("Old Title", result.history[0].title)
        assertEquals("New Title", result.title)
    }

    @Test
    fun recordModification_appendsToExistingHistory() {
        val manager = EntryHistoryManager(historyMaxItems = 10, historyMaxSize = -1)

        val v1 = entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z"))
        val v2 = entry("e1", "V2", modTime = time("2024-02-01T00:00:00Z"), history = listOf(v1))
        val v3 = entry("e1", "V3", modTime = time("2024-03-01T00:00:00Z"), history = v2.history)

        val result = manager.recordModification(v3, v2)

        assertEquals(2, result.history.size)
        assertEquals("V1", result.history[0].title)
        assertEquals("V2", result.history[1].title)
    }

    @Test
    fun recordModification_previousVersionHistoryIsStripped() {
        val manager = EntryHistoryManager(historyMaxItems = 10, historyMaxSize = -1)

        val v1 = entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z"))
        val previousWithHistory =
            entry(
                "e1",
                "V2",
                modTime = time("2024-02-01T00:00:00Z"),
                history = listOf(v1),
            )
        val current = entry("e1", "V3", modTime = time("2024-03-01T00:00:00Z"))

        val result = manager.recordModification(current, previousWithHistory)

        // The snapshot of previousWithHistory in history should have empty history
        assertEquals(1, result.history.size)
        assertTrue(result.history[0].history.isEmpty())
    }

    // -- pruneHistory: by item count --

    @Test
    fun pruneHistory_withinLimits_unchanged() {
        val manager = EntryHistoryManager(historyMaxItems = 5, historyMaxSize = -1)

        val history =
            listOf(
                entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z")),
                entry("e1", "V2", modTime = time("2024-02-01T00:00:00Z")),
            )

        val result = manager.pruneHistory(history)

        assertEquals(2, result.size)
    }

    @Test
    fun pruneHistory_exceedsMaxItems_removesOldest() {
        val manager = EntryHistoryManager(historyMaxItems = 2, historyMaxSize = -1)

        val history =
            listOf(
                entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z")),
                entry("e1", "V2", modTime = time("2024-02-01T00:00:00Z")),
                entry("e1", "V3", modTime = time("2024-03-01T00:00:00Z")),
                entry("e1", "V4", modTime = time("2024-04-01T00:00:00Z")),
            )

        val result = manager.pruneHistory(history)

        assertEquals(2, result.size)
        assertEquals("V3", result[0].title)
        assertEquals("V4", result[1].title)
    }

    @Test
    fun pruneHistory_maxItemsZero_removesAll() {
        val manager = EntryHistoryManager(historyMaxItems = 0, historyMaxSize = -1)

        val history =
            listOf(
                entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z")),
            )

        val result = manager.pruneHistory(history)

        assertTrue(result.isEmpty())
    }

    @Test
    fun pruneHistory_maxItemsNegative_unlimited() {
        val manager = EntryHistoryManager(historyMaxItems = -1, historyMaxSize = -1)

        val history =
            (1..50).map {
                entry("e1", "V$it", modTime = Instant.fromEpochSeconds(1704067200L + it * 86400L))
            }

        val result = manager.pruneHistory(history)

        assertEquals(50, result.size)
    }

    // -- pruneHistory: by size --

    @Test
    fun pruneHistory_exceedsMaxSize_removesOldest() {
        // Each entry with a large attachment will be sizable
        val largeData = ByteArray(1000)
        val manager = EntryHistoryManager(historyMaxItems = -1, historyMaxSize = 1500)

        val history =
            listOf(
                entry(
                    "e1",
                    "V1",
                    modTime = time("2024-01-01T00:00:00Z"),
                    attachments = listOf(KdbxAttachment(0, "file.bin", largeData)),
                ),
                entry(
                    "e1",
                    "V2",
                    modTime = time("2024-02-01T00:00:00Z"),
                    attachments = listOf(KdbxAttachment(1, "file.bin", largeData)),
                ),
                entry(
                    "e1",
                    "V3",
                    modTime = time("2024-03-01T00:00:00Z"),
                    attachments = listOf(KdbxAttachment(2, "file.bin", largeData)),
                ),
            )

        val result = manager.pruneHistory(history)

        // Each entry is ~1000+ bytes, so only 1 should fit within 1500 limit
        assertEquals(1, result.size)
        assertEquals("V3", result[0].title)
    }

    @Test
    fun pruneHistory_maxSizeNegative_unlimited() {
        val largeData = ByteArray(10_000)
        val manager = EntryHistoryManager(historyMaxItems = -1, historyMaxSize = -1)

        val history =
            listOf(
                entry("e1", "V1", attachments = listOf(KdbxAttachment(0, "f", largeData))),
                entry("e1", "V2", attachments = listOf(KdbxAttachment(1, "f", largeData))),
            )

        val result = manager.pruneHistory(history)

        assertEquals(2, result.size)
    }

    // -- pruneHistory: combined limits --

    @Test
    fun pruneHistory_bothLimits_mostRestrictiveWins() {
        val largeData = ByteArray(2000)
        // Max 5 items, but max size only fits ~2 entries
        val manager = EntryHistoryManager(historyMaxItems = 5, historyMaxSize = 5000)

        val history =
            (1..5).map {
                entry(
                    "e1",
                    "V$it",
                    modTime = time("2024-0$it-01T00:00:00Z"),
                    attachments = listOf(KdbxAttachment(it, "file.bin", largeData)),
                )
            }

        val result = manager.pruneHistory(history)

        // Size limit should be more restrictive than item count
        assertTrue(result.size < 5)
        // Newest entries should be kept
        assertEquals("V5", result.last().title)
    }

    // -- pruneHistory: sorting --

    @Test
    fun pruneHistory_unsortedInput_sortedByModificationTime() {
        val manager = EntryHistoryManager(historyMaxItems = -1, historyMaxSize = -1)

        val history =
            listOf(
                entry("e1", "V3", modTime = time("2024-03-01T00:00:00Z")),
                entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z")),
                entry("e1", "V2", modTime = time("2024-02-01T00:00:00Z")),
            )

        val result = manager.pruneHistory(history)

        assertEquals("V1", result[0].title)
        assertEquals("V2", result[1].title)
        assertEquals("V3", result[2].title)
    }

    @Test
    fun pruneHistory_nullModificationTime_treatedAsOldest() {
        val manager = EntryHistoryManager(historyMaxItems = 2, historyMaxSize = -1)

        val history =
            listOf(
                entry("e1", "V1", modTime = null),
                entry("e1", "V2", modTime = time("2024-02-01T00:00:00Z")),
                entry("e1", "V3", modTime = time("2024-03-01T00:00:00Z")),
            )

        val result = manager.pruneHistory(history)

        assertEquals(2, result.size)
        assertEquals("V2", result[0].title)
        assertEquals("V3", result[1].title)
    }

    @Test
    fun pruneHistory_emptyList_returnsEmpty() {
        val manager = EntryHistoryManager(historyMaxItems = 5, historyMaxSize = -1)

        val result = manager.pruneHistory(emptyList())

        assertTrue(result.isEmpty())
    }

    // -- pruneGroupHistory --

    @Test
    fun pruneGroupHistory_prunesAllEntriesInTree() {
        val manager = EntryHistoryManager(historyMaxItems = 1, historyMaxSize = -1)

        val entryWithHistory =
            entry(
                "e1",
                "Current",
                modTime = time("2024-03-01T00:00:00Z"),
                history =
                listOf(
                    entry("e1", "V1", modTime = time("2024-01-01T00:00:00Z")),
                    entry("e1", "V2", modTime = time("2024-02-01T00:00:00Z")),
                ),
            )

        val nestedEntryWithHistory =
            entry(
                "e2",
                "Nested",
                modTime = time("2024-03-01T00:00:00Z"),
                history =
                listOf(
                    entry("e2", "N1", modTime = time("2024-01-01T00:00:00Z")),
                    entry("e2", "N2", modTime = time("2024-02-01T00:00:00Z")),
                    entry("e2", "N3", modTime = time("2024-02-15T00:00:00Z")),
                ),
            )

        val group =
            KdbxGroup(
                uuid = "root",
                name = "Root",
                entries = listOf(entryWithHistory),
                groups =
                listOf(
                    KdbxGroup(
                        uuid = "sub",
                        name = "Sub",
                        entries = listOf(nestedEntryWithHistory),
                    ),
                ),
            )

        val result = manager.pruneGroupHistory(group)

        assertEquals(1, result.entries[0].history.size)
        assertEquals("V2", result.entries[0].history[0].title)

        assertEquals(
            1,
            result.groups[0]
                .entries[0]
                .history.size,
        )
        assertEquals(
            "N3",
            result.groups[0]
                .entries[0]
                .history[0]
                .title,
        )
    }

    @Test
    fun pruneGroupHistory_entriesWithoutHistory_unchanged() {
        val manager = EntryHistoryManager(historyMaxItems = 1, historyMaxSize = -1)

        val entryNoHistory = entry("e1", "No History")
        val group = KdbxGroup(uuid = "root", name = "Root", entries = listOf(entryNoHistory))

        val result = manager.pruneGroupHistory(group)

        assertTrue(result.entries[0].history.isEmpty())
    }

    // -- estimateEntrySize --

    @Test
    fun estimateEntrySize_emptyEntry() {
        val e = KdbxEntry(uuid = "test")
        val size = EntryHistoryManager.estimateEntrySize(e)

        // Should be base overhead only
        assertEquals(128L, size)
    }

    @Test
    fun estimateEntrySize_withFields() {
        val e =
            KdbxEntry(
                uuid = "test",
                fields =
                listOf(
                    KdbxEntryField("Title", "My Entry"),
                    KdbxEntryField("Password", "secret123", isProtected = true),
                ),
            )
        val size = EntryHistoryManager.estimateEntrySize(e)

        // Base + field strings (key + value) * 2 for UTF-16
        val expectedFieldSize =
            (
                "Title".length + "My Entry".length +
                    "Password".length + "secret123".length
                ) * 2L
        assertEquals(128L + expectedFieldSize, size)
    }

    @Test
    fun estimateEntrySize_withAttachments() {
        val data = ByteArray(5000)
        val e =
            KdbxEntry(
                uuid = "test",
                attachments = listOf(KdbxAttachment(0, "photo.jpg", data)),
            )
        val size = EntryHistoryManager.estimateEntrySize(e)

        val expectedAttachmentSize = "photo.jpg".length * 2L + 5000L
        assertEquals(128L + expectedAttachmentSize, size)
    }

    @Test
    fun estimateEntrySize_withTags() {
        val e =
            KdbxEntry(
                uuid = "test",
                tags = listOf("work", "important"),
            )
        val size = EntryHistoryManager.estimateEntrySize(e)

        val expectedTagSize = ("work".length + "important".length) * 2L
        assertEquals(128L + expectedTagSize, size)
    }

    // -- Constructor from KdbxMeta --

    @Test
    fun constructorFromMeta_usesMetaValues() {
        val meta = KdbxMeta(historyMaxItems = 5, historyMaxSize = 1024)
        val manager = EntryHistoryManager(meta)

        val history =
            (1..10).map {
                entry("e1", "V$it", modTime = time("2024-01-${it.toString().padStart(2, '0')}T00:00:00Z"))
            }

        val result = manager.pruneHistory(history)

        assertEquals(5, result.size)
    }

    // -- Helpers --

    private fun entry(
        uuid: String,
        title: String,
        modTime: Instant? = null,
        history: List<KdbxEntry> = emptyList(),
        attachments: List<KdbxAttachment> = emptyList(),
    ) = KdbxEntry(
        uuid = uuid,
        fields = listOf(KdbxEntryField(KdbxEntry.FIELD_TITLE, title)),
        lastModificationTime = modTime,
        history = history,
        attachments = attachments,
    )

    private fun time(iso: String): Instant = Instant.parse(iso)
}
