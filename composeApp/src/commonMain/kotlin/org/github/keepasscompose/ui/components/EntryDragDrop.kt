package org.github.keepasscompose.ui.components

import org.github.keepasscompose.core.model.KdbxEntry
import org.github.keepasscompose.core.model.KdbxGroup

data class EntryMoveRequest(val entryUuids: List<String>, val targetGroupUuid: String)

/**
 * Applies an entry move by removing entries from their current group
 * and adding them to the target group.
 * Returns the modified root group, or null if the target group doesn't exist.
 */
fun applyEntryMove(root: KdbxGroup, move: EntryMoveRequest): KdbxGroup? {
    if (!groupExists(root, move.targetGroupUuid)) return null

    val uuidSet = move.entryUuids.toSet()

    // Collect entries to move
    val entriesToMove = collectEntries(root, uuidSet)
    if (entriesToMove.isEmpty()) return null

    // Remove from current locations
    val afterRemoval = removeEntries(root, uuidSet)

    // Insert into target group
    return insertEntries(afterRemoval, move.targetGroupUuid, entriesToMove)
}

private fun groupExists(group: KdbxGroup, uuid: String): Boolean {
    if (group.uuid == uuid) return true
    return group.groups.any { groupExists(it, uuid) }
}

private fun collectEntries(group: KdbxGroup, uuids: Set<String>): List<KdbxEntry> = buildList {
    addAll(group.entries.filter { it.uuid in uuids })
    group.groups.forEach { addAll(collectEntries(it, uuids)) }
}

private fun removeEntries(group: KdbxGroup, uuids: Set<String>): KdbxGroup = group.copy(
    entries = group.entries.filter { it.uuid !in uuids },
    groups = group.groups.map { removeEntries(it, uuids) },
)

private fun insertEntries(group: KdbxGroup, targetUuid: String, entries: List<KdbxEntry>): KdbxGroup {
    if (group.uuid == targetUuid) {
        return group.copy(entries = group.entries + entries)
    }
    return group.copy(
        groups = group.groups.map { insertEntries(it, targetUuid, entries) },
    )
}
