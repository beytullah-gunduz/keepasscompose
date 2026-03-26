package org.github.keepasscompose.ui.components

import org.github.keepasscompose.core.model.KdbxGroup

data class GroupMoveRequest(
    val sourceGroupUuid: String,
    val targetParentUuid: String,
    val targetIndex: Int,
)

/**
 * Validates whether a group can be moved to the target parent.
 * Prevents moving a group into itself or any of its descendants.
 */
fun isValidGroupMove(root: KdbxGroup, sourceUuid: String, targetParentUuid: String): Boolean {
    if (sourceUuid == targetParentUuid) return false
    val sourceGroup = findGroup(root, sourceUuid) ?: return false
    return !isDescendant(sourceGroup, targetParentUuid)
}

/**
 * Applies a group move by removing the source group from its current parent
 * and inserting it under the target parent at the specified index.
 * Returns the modified root group, or null if the move is invalid.
 */
fun applyGroupMove(root: KdbxGroup, move: GroupMoveRequest): KdbxGroup? {
    if (!isValidGroupMove(root, move.sourceGroupUuid, move.targetParentUuid)) return null

    val sourceGroup = findGroup(root, move.sourceGroupUuid) ?: return null

    // Remove from current parent
    val afterRemoval = removeGroup(root, move.sourceGroupUuid) ?: return null

    // Insert into target parent
    return insertGroup(afterRemoval, move.targetParentUuid, sourceGroup, move.targetIndex)
}

private fun findGroup(group: KdbxGroup, uuid: String): KdbxGroup? {
    if (group.uuid == uuid) return group
    for (child in group.groups) {
        findGroup(child, uuid)?.let { return it }
    }
    return null
}

private fun isDescendant(parent: KdbxGroup, targetUuid: String): Boolean {
    for (child in parent.groups) {
        if (child.uuid == targetUuid) return true
        if (isDescendant(child, targetUuid)) return true
    }
    return false
}

private fun removeGroup(group: KdbxGroup, uuid: String): KdbxGroup? {
    val newChildren = group.groups.filter { it.uuid != uuid }.map {
        removeGroup(it, uuid) ?: it
    }
    return group.copy(groups = newChildren)
}

private fun insertGroup(
    group: KdbxGroup,
    targetParentUuid: String,
    toInsert: KdbxGroup,
    index: Int,
): KdbxGroup {
    if (group.uuid == targetParentUuid) {
        val newChildren = group.groups.toMutableList()
        val clampedIndex = index.coerceIn(0, newChildren.size)
        newChildren.add(clampedIndex, toInsert)
        return group.copy(groups = newChildren)
    }
    return group.copy(
        groups = group.groups.map { insertGroup(it, targetParentUuid, toInsert, index) },
    )
}
