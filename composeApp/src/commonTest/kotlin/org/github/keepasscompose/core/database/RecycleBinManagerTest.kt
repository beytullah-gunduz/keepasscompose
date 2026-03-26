package org.github.keepasscompose.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxEntryField
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxMeta

class RecycleBinManagerTest {

    private val now = Instant.parse("2024-06-15T12:00:00Z")

    // -- deleteEntry: recycle bin enabled --

    @Test
    fun deleteEntry_movesToRecycleBin() {
        val entry = entry("e1", "Gmail")
        val root = rootWith(entries = listOf(entry))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.deleteEntry(root, "e1", now)

        // Entry removed from root
        assertTrue(result.rootGroup.entries.isEmpty())
        // Entry in recycle bin
        val bin = manager(result).findRecycleBin(result.rootGroup)
        assertNotNull(bin)
        assertEquals(1, bin.entries.size)
        assertEquals("Gmail", bin.entries[0].title)
        // No tombstones (soft delete)
        assertTrue(result.deletedObjects.isEmpty())
    }

    @Test
    fun deleteEntry_createsRecycleBinOnFirstDelete() {
        val entry = entry("e1", "Gmail")
        val root = rootWith(entries = listOf(entry))
        val meta = KdbxMeta(recycleBinEnabled = true, recycleBinUuid = null)
        val manager = RecycleBinManager(meta)

        val result = manager.deleteEntry(root, "e1", now)

        // Meta now has recycle bin UUID
        assertNotNull(result.meta.recycleBinUuid)
        // Recycle bin group created
        val bin = RecycleBinManager(result.meta).findRecycleBin(result.rootGroup)
        assertNotNull(bin)
        assertEquals(RecycleBinManager.RECYCLE_BIN_NAME, bin.name)
        assertEquals(RecycleBinManager.RECYCLE_BIN_ICON, bin.icon.standardIndex)
        assertEquals(1, bin.entries.size)
    }

    @Test
    fun deleteEntry_fromNestedGroup() {
        val entry = entry("e1", "Nested Entry")
        val subGroup = KdbxGroup(uuid = "sub", name = "Sub", entries = listOf(entry))
        val root = rootWith(groups = listOf(subGroup))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.deleteEntry(root, "e1", now)

        // Removed from nested group
        assertTrue(result.rootGroup.groups[0].entries.isEmpty())
        // Added to recycle bin
        val bin = manager(result).findRecycleBin(result.rootGroup)
        assertNotNull(bin)
        assertEquals(1, bin.entries.size)
    }

    @Test
    fun deleteEntry_nonExistent_noChange() {
        val root = rootWith()
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.deleteEntry(root, "nonexistent", now)

        assertEquals(root, result.rootGroup)
        assertTrue(result.deletedObjects.isEmpty())
    }

    // -- deleteEntry: recycle bin disabled --

    @Test
    fun deleteEntry_recycleBinDisabled_permanentDelete() {
        val entry = entry("e1", "Gmail")
        val root = rootWith(entries = listOf(entry))
        val manager = RecycleBinManager(KdbxMeta(recycleBinEnabled = false))

        val result = manager.deleteEntry(root, "e1", now)

        assertTrue(result.rootGroup.entries.isEmpty())
        assertEquals(1, result.deletedObjects.size)
        assertEquals("e1", result.deletedObjects[0].uuid)
        assertEquals(now, result.deletedObjects[0].deletionTime)
    }

    // -- deleteGroup: recycle bin enabled --

    @Test
    fun deleteGroup_movesToRecycleBin() {
        val subGroup = KdbxGroup(
            uuid = "sub1",
            name = "Work",
            entries = listOf(entry("e1", "Work Email")),
        )
        val root = rootWith(groups = listOf(subGroup, recycleBinGroup()))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.deleteGroup(root, "sub1", now)

        // Group removed from root (only recycle bin remains)
        val nonBinGroups = result.rootGroup.groups.filter { it.uuid != RECYCLE_BIN_UUID }
        assertTrue(nonBinGroups.isEmpty())
        // Group in recycle bin with its entries intact
        val bin = manager(result).findRecycleBin(result.rootGroup)
        assertNotNull(bin)
        assertEquals(1, bin.groups.size)
        assertEquals("Work", bin.groups[0].name)
        assertEquals(1, bin.groups[0].entries.size)
        // No tombstones
        assertTrue(result.deletedObjects.isEmpty())
    }

    @Test
    fun deleteGroup_cannotDeleteRecycleBinItself() {
        val root = rootWith(groups = listOf(recycleBinGroup()))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.deleteGroup(root, RECYCLE_BIN_UUID, now)

        // No change
        assertEquals(1, result.rootGroup.groups.size)
        assertEquals(RECYCLE_BIN_UUID, result.rootGroup.groups[0].uuid)
    }

    @Test
    fun deleteGroup_createsRecycleBinOnFirstDelete() {
        val subGroup = KdbxGroup(uuid = "sub1", name = "Work")
        val root = rootWith(groups = listOf(subGroup))
        val meta = KdbxMeta(recycleBinEnabled = true, recycleBinUuid = null)
        val manager = RecycleBinManager(meta)

        val result = manager.deleteGroup(root, "sub1", now)

        assertNotNull(result.meta.recycleBinUuid)
        val bin = RecycleBinManager(result.meta).findRecycleBin(result.rootGroup)
        assertNotNull(bin)
        assertEquals(1, bin.groups.size)
        assertEquals("Work", bin.groups[0].name)
    }

    // -- deleteGroup: recycle bin disabled --

    @Test
    fun deleteGroup_recycleBinDisabled_permanentDelete() {
        val subGroup = KdbxGroup(
            uuid = "sub1",
            name = "Work",
            entries = listOf(entry("e1", "Entry1")),
            groups = listOf(
                KdbxGroup(uuid = "sub2", name = "Nested", entries = listOf(entry("e2", "Entry2")))
            ),
        )
        val root = rootWith(groups = listOf(subGroup))
        val manager = RecycleBinManager(KdbxMeta(recycleBinEnabled = false))

        val result = manager.deleteGroup(root, "sub1", now)

        assertTrue(result.rootGroup.groups.isEmpty())
        // Tombstones for group + all contents
        val uuids = result.deletedObjects.map { it.uuid }.toSet()
        assertEquals(setOf("sub1", "e1", "sub2", "e2"), uuids)
        assertTrue(result.deletedObjects.all { it.deletionTime == now })
    }

    @Test
    fun deleteGroup_nonExistent_noChange() {
        val root = rootWith()
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.deleteGroup(root, "nonexistent", now)

        assertEquals(root, result.rootGroup)
    }

    // -- emptyRecycleBin --

    @Test
    fun emptyRecycleBin_removesAllContents() {
        val bin = recycleBinGroup(
            entries = listOf(entry("e1", "Deleted1"), entry("e2", "Deleted2")),
            groups = listOf(KdbxGroup(uuid = "dg1", name = "Deleted Group")),
        )
        val root = rootWith(groups = listOf(bin))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.emptyRecycleBin(root, now)

        // Recycle bin still exists but is empty
        val emptyBin = RecycleBinManager(metaWithRecycleBin()).findRecycleBin(result.rootGroup)
        assertNotNull(emptyBin)
        assertTrue(emptyBin.entries.isEmpty())
        assertTrue(emptyBin.groups.isEmpty())
        // Tombstones for all contents
        val uuids = result.deletedObjects.map { it.uuid }.toSet()
        assertEquals(setOf("e1", "e2", "dg1"), uuids)
    }

    @Test
    fun emptyRecycleBin_nestedContents() {
        val nestedGroup = KdbxGroup(
            uuid = "ng1",
            name = "Nested",
            entries = listOf(entry("ne1", "Nested Entry")),
        )
        val bin = recycleBinGroup(
            groups = listOf(nestedGroup),
        )
        val root = rootWith(groups = listOf(bin))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.emptyRecycleBin(root, now)

        val uuids = result.deletedObjects.map { it.uuid }.toSet()
        assertEquals(setOf("ng1", "ne1"), uuids)
    }

    @Test
    fun emptyRecycleBin_alreadyEmpty() {
        val bin = recycleBinGroup()
        val root = rootWith(groups = listOf(bin))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val result = manager.emptyRecycleBin(root, now)

        assertTrue(result.deletedObjects.isEmpty())
    }

    @Test
    fun emptyRecycleBin_noRecycleBinExists() {
        val root = rootWith()
        val manager = RecycleBinManager(KdbxMeta(recycleBinEnabled = true, recycleBinUuid = null))

        val result = manager.emptyRecycleBin(root, now)

        assertTrue(result.deletedObjects.isEmpty())
        assertEquals(root, result.rootGroup)
    }

    // -- findRecycleBin --

    @Test
    fun findRecycleBin_exists() {
        val bin = recycleBinGroup()
        val root = rootWith(groups = listOf(bin))
        val manager = RecycleBinManager(metaWithRecycleBin())

        val found = manager.findRecycleBin(root)

        assertNotNull(found)
        assertEquals(RECYCLE_BIN_UUID, found.uuid)
    }

    @Test
    fun findRecycleBin_noUuidInMeta() {
        val root = rootWith()
        val manager = RecycleBinManager(KdbxMeta(recycleBinUuid = null))

        assertNull(manager.findRecycleBin(root))
    }

    @Test
    fun findRecycleBin_uuidInMetaButGroupMissing() {
        val root = rootWith()
        val manager = RecycleBinManager(metaWithRecycleBin())

        assertNull(manager.findRecycleBin(root))
    }

    // -- Tree operation helpers --

    @Test
    fun findEntry_inNestedGroup() {
        val entry = entry("e1", "Deep Entry")
        val sub2 = KdbxGroup(uuid = "sub2", name = "Sub2", entries = listOf(entry))
        val sub1 = KdbxGroup(uuid = "sub1", name = "Sub1", groups = listOf(sub2))
        val root = rootWith(groups = listOf(sub1))

        val found = RecycleBinManager.findEntry(root, "e1")

        assertNotNull(found)
        assertEquals("Deep Entry", found.title)
    }

    @Test
    fun removeEntry_fromNestedGroup() {
        val entry = entry("e1", "Entry")
        val sub = KdbxGroup(uuid = "sub", name = "Sub", entries = listOf(entry))
        val root = rootWith(groups = listOf(sub))

        val result = RecycleBinManager.removeEntry(root, "e1")

        assertNotNull(result)
        assertTrue(result.groups[0].entries.isEmpty())
    }

    @Test
    fun removeGroup_fromRoot() {
        val sub = KdbxGroup(uuid = "sub", name = "Sub")
        val root = rootWith(groups = listOf(sub))

        val result = RecycleBinManager.removeGroup(root, "sub")

        assertNotNull(result)
        assertTrue(result.groups.isEmpty())
    }

    @Test
    fun addEntryToGroup_nestedTarget() {
        val sub = KdbxGroup(uuid = "sub", name = "Sub")
        val root = rootWith(groups = listOf(sub))

        val result = RecycleBinManager.addEntryToGroup(root, "sub", entry("e1", "New"))

        assertEquals(1, result.groups[0].entries.size)
        assertEquals("New", result.groups[0].entries[0].title)
    }

    // -- Helpers --

    private fun entry(uuid: String, title: String) = KdbxEntry(
        uuid = uuid,
        fields = listOf(KdbxEntryField(KdbxEntry.FIELD_TITLE, title)),
    )

    private fun rootWith(
        entries: List<KdbxEntry> = emptyList(),
        groups: List<KdbxGroup> = emptyList(),
    ) = KdbxGroup(uuid = "root", name = "Root", entries = entries, groups = groups)

    private fun recycleBinGroup(
        entries: List<KdbxEntry> = emptyList(),
        groups: List<KdbxGroup> = emptyList(),
    ) = KdbxGroup(
        uuid = RECYCLE_BIN_UUID,
        name = RecycleBinManager.RECYCLE_BIN_NAME,
        entries = entries,
        groups = groups,
    )

    private fun metaWithRecycleBin() = KdbxMeta(
        recycleBinEnabled = true,
        recycleBinUuid = RECYCLE_BIN_UUID,
    )

    /** Create a new manager from a result's meta, for chaining assertions. */
    private fun manager(result: RecycleBinManager.DeleteResult) =
        RecycleBinManager(result.meta)

    companion object {
        private const val RECYCLE_BIN_UUID = "cmVjeWNsZWJpbg=="
    }
}
