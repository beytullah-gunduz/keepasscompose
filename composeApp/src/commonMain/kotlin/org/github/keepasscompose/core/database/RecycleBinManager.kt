package org.github.keepasscompose.core.database

import org.github.keepasscompose.core.model.DeletedObject
import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup
import org.github.keepasscompose.core.model.KdbxIcon
import org.github.keepasscompose.core.model.KdbxMeta
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Manages the KeePass Recycle Bin: moving deleted entries and groups to a
 * special recycle bin group instead of permanently deleting them.
 *
 * When `recycleBinEnabled` is false, delete operations remove items permanently
 * and record [DeletedObject] tombstones.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalUuidApi::class)
class RecycleBinManager(private val meta: KdbxMeta) {
    /**
     * Result of a delete operation. Contains the updated group tree,
     * updated metadata (may include a new recycle bin UUID), and any
     * deleted-object tombstones (only for permanent deletions).
     */
    data class DeleteResult(val rootGroup: KdbxGroup, val meta: KdbxMeta, val deletedObjects: List<DeletedObject> = emptyList())

    /**
     * Result of emptying the recycle bin. Contains the updated group tree
     * and tombstones for all permanently deleted items.
     */
    data class EmptyResult(val rootGroup: KdbxGroup, val deletedObjects: List<DeletedObject>)

    /**
     * Deletes an entry by moving it to the recycle bin, or permanently
     * if the recycle bin is disabled.
     *
     * @param rootGroup The current root group tree.
     * @param entryUuid UUID of the entry to delete.
     * @param now Current timestamp for tombstones and recycle bin creation.
     * @return Updated state after the deletion.
     */
    fun deleteEntry(rootGroup: KdbxGroup, entryUuid: String, now: Instant): DeleteResult {
        if (!meta.recycleBinEnabled) {
            val updated = removeEntry(rootGroup, entryUuid) ?: return noChange(rootGroup)
            return DeleteResult(
                rootGroup = updated,
                meta = meta,
                deletedObjects = listOf(DeletedObject(uuid = entryUuid, deletionTime = now)),
            )
        }

        val entry = findEntry(rootGroup, entryUuid) ?: return noChange(rootGroup)
        val withoutEntry = removeEntry(rootGroup, entryUuid) ?: return noChange(rootGroup)

        val (groupWithBin, updatedMeta) = ensureRecycleBin(withoutEntry, now)
        val recycleBinUuid = updatedMeta.recycleBinUuid!!

        val result = addEntryToGroup(groupWithBin, recycleBinUuid, entry)
        return DeleteResult(rootGroup = result, meta = updatedMeta)
    }

    /**
     * Deletes a group by moving it to the recycle bin, or permanently
     * if the recycle bin is disabled.
     *
     * The recycle bin group itself cannot be moved to the recycle bin.
     *
     * @param rootGroup The current root group tree.
     * @param groupUuid UUID of the group to delete.
     * @param now Current timestamp for tombstones and recycle bin creation.
     * @return Updated state after the deletion.
     */
    fun deleteGroup(rootGroup: KdbxGroup, groupUuid: String, now: Instant): DeleteResult {
        // Cannot delete the recycle bin group itself via this method
        if (groupUuid == meta.recycleBinUuid) return noChange(rootGroup)

        if (!meta.recycleBinEnabled) {
            val group = findGroup(rootGroup, groupUuid) ?: return noChange(rootGroup)
            val updated = removeGroup(rootGroup, groupUuid) ?: return noChange(rootGroup)
            return DeleteResult(
                rootGroup = updated,
                meta = meta,
                deletedObjects = collectUuids(group, now),
            )
        }

        val group = findGroup(rootGroup, groupUuid) ?: return noChange(rootGroup)
        val withoutGroup = removeGroup(rootGroup, groupUuid) ?: return noChange(rootGroup)

        val (groupWithBin, updatedMeta) = ensureRecycleBin(withoutGroup, now)
        val recycleBinUuid = updatedMeta.recycleBinUuid!!

        val result = addGroupToGroup(groupWithBin, recycleBinUuid, group)
        return DeleteResult(rootGroup = result, meta = updatedMeta)
    }

    /**
     * Empties the recycle bin, permanently deleting all its contents
     * and recording tombstones for each deleted item.
     *
     * @param rootGroup The current root group tree.
     * @param now Current timestamp for tombstones.
     * @return Updated state with empty recycle bin and tombstones.
     */
    fun emptyRecycleBin(rootGroup: KdbxGroup, now: Instant): EmptyResult {
        val recycleBinUuid =
            meta.recycleBinUuid
                ?: return EmptyResult(rootGroup = rootGroup, deletedObjects = emptyList())

        val recycleBin =
            findGroup(rootGroup, recycleBinUuid)
                ?: return EmptyResult(rootGroup = rootGroup, deletedObjects = emptyList())

        val tombstones = collectContentsUuids(recycleBin, now)

        val cleared =
            replaceGroup(rootGroup, recycleBinUuid) { bin ->
                bin.copy(entries = emptyList(), groups = emptyList())
            }

        return EmptyResult(rootGroup = cleared, deletedObjects = tombstones)
    }

    /**
     * Finds the recycle bin group in the tree, if it exists.
     */
    fun findRecycleBin(rootGroup: KdbxGroup): KdbxGroup? {
        val uuid = meta.recycleBinUuid ?: return null
        return findGroup(rootGroup, uuid)
    }

    // -- Internal helpers --

    private fun noChange(rootGroup: KdbxGroup) = DeleteResult(rootGroup = rootGroup, meta = meta)

    /**
     * Ensures a recycle bin group exists. Creates one if needed.
     * Returns the updated root group and meta.
     */
    private fun ensureRecycleBin(rootGroup: KdbxGroup, now: Instant): Pair<KdbxGroup, KdbxMeta> {
        val existingUuid = meta.recycleBinUuid
        if (existingUuid != null && findGroup(rootGroup, existingUuid) != null) {
            return rootGroup to meta
        }

        val newUuid = generateUuid()
        val recycleBin =
            KdbxGroup(
                uuid = newUuid,
                name = RECYCLE_BIN_NAME,
                icon = KdbxIcon(standardIndex = RECYCLE_BIN_ICON),
            )

        val updatedRoot = rootGroup.copy(groups = rootGroup.groups + recycleBin)
        val updatedMeta = meta.copy(recycleBinUuid = newUuid)
        return updatedRoot to updatedMeta
    }

    companion object {
        const val RECYCLE_BIN_NAME = "Recycle Bin"
        const val RECYCLE_BIN_ICON = 43

        internal fun generateUuid(): String = Base64.encode(Uuid.random().toByteArray())

        /** Find an entry by UUID anywhere in the group tree. */
        fun findEntry(group: KdbxGroup, entryUuid: String): KdbxEntry? {
            group.entries.firstOrNull { it.uuid == entryUuid }?.let { return it }
            for (sub in group.groups) {
                findEntry(sub, entryUuid)?.let { return it }
            }
            return null
        }

        /** Find a group by UUID anywhere in the group tree (not including root itself). */
        fun findGroup(root: KdbxGroup, groupUuid: String): KdbxGroup? {
            if (root.uuid == groupUuid) return root
            for (sub in root.groups) {
                findGroup(sub, groupUuid)?.let { return it }
            }
            return null
        }

        /** Remove an entry by UUID from the tree. Returns null if not found. */
        fun removeEntry(group: KdbxGroup, entryUuid: String): KdbxGroup? {
            val hasEntry = group.entries.any { it.uuid == entryUuid }
            if (hasEntry) {
                return group.copy(entries = group.entries.filter { it.uuid != entryUuid })
            }
            var found = false
            val updatedGroups =
                group.groups.map { sub ->
                    val result = removeEntry(sub, entryUuid)
                    if (result != null) {
                        found = true
                        result
                    } else {
                        sub
                    }
                }
            return if (found) group.copy(groups = updatedGroups) else null
        }

        /** Remove a group by UUID from the tree. Returns null if not found. */
        fun removeGroup(group: KdbxGroup, groupUuid: String): KdbxGroup? {
            val hasGroup = group.groups.any { it.uuid == groupUuid }
            if (hasGroup) {
                return group.copy(groups = group.groups.filter { it.uuid != groupUuid })
            }
            var found = false
            val updatedGroups =
                group.groups.map { sub ->
                    val result = removeGroup(sub, groupUuid)
                    if (result != null) {
                        found = true
                        result
                    } else {
                        sub
                    }
                }
            return if (found) group.copy(groups = updatedGroups) else null
        }

        /** Add an entry to a specific group by UUID. */
        fun addEntryToGroup(root: KdbxGroup, targetUuid: String, entry: KdbxEntry): KdbxGroup {
            if (root.uuid == targetUuid) {
                return root.copy(entries = root.entries + entry)
            }
            return root.copy(
                groups =
                root.groups.map { sub ->
                    addEntryToGroup(sub, targetUuid, entry)
                },
            )
        }

        /** Add a child group to a specific group by UUID. */
        fun addGroupToGroup(root: KdbxGroup, targetUuid: String, group: KdbxGroup): KdbxGroup {
            if (root.uuid == targetUuid) {
                return root.copy(groups = root.groups + group)
            }
            return root.copy(
                groups =
                root.groups.map { sub ->
                    addGroupToGroup(sub, targetUuid, group)
                },
            )
        }

        /** Replace a group in the tree using a transform function. */
        fun replaceGroup(root: KdbxGroup, targetUuid: String, transform: (KdbxGroup) -> KdbxGroup): KdbxGroup {
            if (root.uuid == targetUuid) return transform(root)
            return root.copy(
                groups =
                root.groups.map { sub ->
                    replaceGroup(sub, targetUuid, transform)
                },
            )
        }

        /** Collect tombstones for a group and all its contents (entries + subgroups). */
        fun collectUuids(group: KdbxGroup, now: Instant): List<DeletedObject> {
            val result = mutableListOf<DeletedObject>()
            result.add(DeletedObject(uuid = group.uuid, deletionTime = now))
            collectContentsUuidsInto(group, now, result)
            return result
        }

        /** Collect tombstones for a group's contents only (not the group itself). */
        fun collectContentsUuids(group: KdbxGroup, now: Instant): List<DeletedObject> {
            val result = mutableListOf<DeletedObject>()
            collectContentsUuidsInto(group, now, result)
            return result
        }

        private fun collectContentsUuidsInto(group: KdbxGroup, now: Instant, result: MutableList<DeletedObject>) {
            for (entry in group.entries) {
                result.add(DeletedObject(uuid = entry.uuid, deletionTime = now))
            }
            for (sub in group.groups) {
                result.add(DeletedObject(uuid = sub.uuid, deletionTime = now))
                collectContentsUuidsInto(sub, now, result)
            }
        }
    }
}
